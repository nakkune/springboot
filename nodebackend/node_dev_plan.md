# Node.js Backend Migration & Development Plan (node_dev_plan.md)

이 문서는 기존 Spring Boot / PostgreSQL 기반의 프로젝트 관리 및 ERP 통합 백엔드 시스템을 Node.js 환경에서 매끄럽게 구동할 수 있도록 이식하고 개발하기 위한 아키텍처 및 상세 개발 계획서입니다. 기존 소스 코드를 수정하지 않고 백엔드를 Node.js로 완벽하게 대체할 수 있도록 설계하였습니다.

---

## 1. 개요 (Overview)
* **목적:** 특정 PC 또는 개발 환경에서 무거운 Java JVM 환경 대신 가볍고 빠른 Node.js 런타임을 통해 백엔드를 구동할 수 있도록 함.
* **호환성 유지:** 기존 Vue 3 프론트엔드(`localhost:5173`)가 바라보는 백엔드 서버(포트: `9090`, Context Path: `/api`) 규격과 100% 호환되어야 함.
* **데이터베이스 공유:** 기존 PostgreSQL 데이터베이스(`localhost:5433/springdb`)의 테이블 스키마 및 트리거를 그대로 사용함.

---

## 2. 기술 스택 선정 및 Spring Boot 매핑

기존 Spring Boot 백엔드의 핵심 구조(Controller-Service-Mapper/Repository)를 가장 유사하게 이식하면서 타입 안정성을 제공하기 위해 **TypeScript** 환경을 기본으로 채택합니다.

개발 환경의 복잡도와 PC 스펙에 따라 두 가지 프레임워크 대안을 제시합니다:

### 대안 A. NestJS + TypeScript (권장)
* **특징:** Spring Boot와 구조(Annotation/Decorator, DI/IoC, Module, Guard, Interceptor)가 매우 흡사하여, Java 개발자가 Node.js 코드를 이해하고 포팅하는 데에 진입장벽이 가장 낮음.
* **스택:** NestJS + Prisma ORM (또는 TypeORM) + class-validator + Passport JWT

### 대안 B. Express.js + TypeScript (경량화)
* **특징:** 설정이 간결하고 런타임 오버헤드가 적어, 사양이 낮은 PC에서도 초고속으로 기동 및 서빙 가능.
* **스택:** Express.js + Prisma ORM + Zod (Validation) + jsonwebtoken + bcryptjs

---

### 기술 스택 매핑 테이블

| 구분 | Spring Boot 스택 | Node.js (NestJS 권장) | Node.js (Express) |
|---|---|---|---|
| **언어/런타임** | Java 17+ (JVM) | TypeScript (Node.js 20+) | TypeScript (Node.js 20+) |
| **웹 프레임워크** | Spring Web MVC | NestJS Core | Express.js |
| **데이터베이스 ORM** | MyBatis (XML Mapper) | **Prisma ORM** | **Prisma ORM** / Knex.js |
| **인증/보안** | Spring Security | `@nestjs/passport` + JWT | `jsonwebtoken` + custom middleware |
| **패스워드 암호화** | BCryptPasswordEncoder | `bcryptjs` (해시 호환) | `bcryptjs` (해시 호환) |
| **유효성 검증** | Spring Bean Validation | `class-validator` | `Zod` or `Joi` |
| **API 문서화** | Springdoc Swagger UI | `@nestjs/swagger` | `swagger-ui-express` |
| **파일 업로드** | MultipartResolver | `@nestjs/platform-express` | `multer` |

---

## 3. 디렉토리 구조 (Directory Structure)

### [권장] NestJS 기반 아키텍처 구조
Spring Boot의 도메인 패키지 방식과 구조적으로 완전히 1:1 매칭되는 도메인 중심 구조입니다.

```text
nodebackend/
├── prisma/
│   └── schema.prisma           # 데이터베이스 스키마 및 테이블 매핑 정의
├── src/
│   ├── main.ts                 # 애플리케이션 엔트리 (포트 9090, 전역 prefix '/api' 설정)
│   ├── app.module.ts           # 루트 모듈
│   ├── common/                 # 공통 모듈 (Guards, Interceptors, Filters, Decorators)
│   │   ├── guards/
│   │   │   └── jwt-auth.guard.ts  # JWT 인증 가드
│   │   └── filters/
│   │       └── http-exception.filter.ts # 전역 예외 처리기 (Spring ControllerAdvice 역할)
│   ├── config/                 # 환경 변수 및 공통 설정
│   └── domains/                # 도메인별 모듈 (Spring 패키지에 직접 대응)
│       ├── user/               # 사용자 및 인증
│       │   ├── user.module.ts
│       │   ├── user.controller.ts
│       │   ├── user.service.ts
│       │   └── dto/            # User DTOs (Pydantic / Pojo 대응)
│       ├── workspace/          # 워크스페이스 및 팀
│       ├── board/              # 보드, 컬럼, 그룹
│       ├── item/               # 아이템, 아이템 값(EAV), 댓글
│       ├── attachment/         # 첨부파일 및 파일 컨트롤러
│       ├── erp/                # ERP 공통 및 하위 모듈
│       │   ├── hr/             # 인사, 급여, 근태, 연차, 평가
│       │   └── sales/          # 영업, 견적서, 세금계산서
│       ├── notification/       # 알림 기능
│       ├── activity/           # 활동 감사 로그
│       └── automation/         # 트리거/액션 자동화
├── uploads/                    # 로컬 업로드 디렉토리 (Spring Boot 파일 업로드 연동)
├── .env                        # 환경 변수 (DATABASE_URL, JWT_SECRET 등)
├── package.json
├── tsconfig.json
└── README.md
```

---

## 4. Spring Boot -> Node.js 마이그레이션 핵심 가이드

### 1) 데이터베이스 접근 (Prisma ORM 설정)
Spring Boot에서는 MyBatis의 XML 매퍼를 통해 유연하게 SQL을 작성했습니다. Node.js로 변환 시, 타입 안전성과 관계형 매핑이 뛰어난 **Prisma ORM**을 권장합니다.
* **Introspection:** 기존 DB가 기동 중일 때 `npx prisma db pull` 명령어를 사용하면 PostgreSQL의 스키마 구조를 자동으로 읽어 `schema.prisma` 파일로 빌드해 줍니다.
* **EAV 데이터 매핑 (item_values):** `item_values` 테이블의 `value_json` 필드는 PostgreSQL의 `JSONB` 타입으로 지정되어 있습니다. Prisma에서는 이를 `Json` 타입으로 다루며, 프론트엔드와의 통신 규격을 맞추기 위해 타입 가드나 DTO 검증기를 거치도록 가이드합니다.

### 2) 비밀번호 해싱 및 인증 호환성 (BCrypt)
Spring Security의 `BCryptPasswordEncoder`는 표준 BCrypt 알고리즘을 사용합니다.
* Node.js의 `bcryptjs` 라이브러리를 사용하면 Java에서 생성된 패스워드 해시 문자열과 완벽하게 호환(비교 검증 및 신규 생성 가능)됩니다.
* **로그인 API (`POST /api/users/login`):** 요청받은 평문 비밀번호를 `bcrypt.compare()` 함수를 사용하여 DB의 `password_hash`와 비교 검증합니다.
* **인증 미들웨어:** 로그인에 성공하면 사용자 ID와 이메일을 담은 JWT 토큰을 서명하여 프론트엔드로 반환하며, 모든 보안 엔드포인트는 `Authorization: Bearer <token>` 헤더를 해석하는 미들웨어/가드를 거치게 설계합니다.

### 3) Context Path 및 포트 세팅
* **서버 포트:** `9090`
* **Context Path:** `/api`
* NestJS의 경우 `main.ts`에서 다음과 같이 설정합니다:
  ```typescript
  const app = await NestFactory.create(AppModule);
  app.setGlobalPrefix('api');
  await app.listen(9090);
  ```
* Express.js의 경우 라우터를 마운트할 때 다음과 같이 설정합니다:
  ```typescript
  app.use('/api', globalRouter);
  app.listen(9090);
  ```

### 4) 파일 업로드 및 정적 자원 서빙 (`/attachments` 및 `/files`)
* Spring Boot에서는 로컬 디렉토리(`uploads/` 등)에 업로드 파일을 저장하고 이를 특정 URL로 서빙합니다.
* Node.js에서는 `multer` 미들웨어를 사용하여 로컬 `uploads/` 폴더에 파일을 업로드하고, `express.static('uploads')`를 통해 프론트엔드에서 `/api/uploads/...` 등의 URL로 다이렉트 접근이 가능하도록 정적 서빙합니다.

---

## 5. 단계별 검증 (Verification Plan)

1. **DB 연결 테스트:** `schema.prisma`를 통해 기존 PostgreSQL(`localhost:5433/springdb`)에 커넥션이 정상적으로 수립되고 쿼리가 조회되는지 확인합니다.
2. **API Endpoint 매핑 확인:** Swagger UI를 기동하여 전체 API 리스트가 기존 Spring Boot의 Swagger 문서와 완전히 일치하는지 비교합니다.
3. **프론트엔드 연동 테스트:** Vue 3 프론트엔드 개발 서버를 띄워두고 백엔드를 Node.js로 변경(포트 9090)한 상태에서 로그인이 정상 실행되고, 보드 및 아이템이 D&D를 거쳐 올바르게 갱신되는지 확인합니다.
