# Node.js Backend Migration Tasklist (node_tasklist.md)

이 문서는 기존 Spring Boot 백엔드 기능을 Node.js(NestJS 또는 Express.js)로 마이그레이션하기 위한 세부 작업 리스트입니다. 각 태스크를 완료할 때마다 체크박스 `[ ]`를 `[x]`로 갱신하여 진척도를 관리하세요.

---

## 📅 Phase 1: 개발 환경 구성 및 기초 인프라 (Core Setup)
- [ ] **Node.js 프로젝트 초기화**
  - [ ] `npm init -y` 및 TypeScript 설정 (`tsconfig.json` 파일 구성)
  - [ ] 필수 의존성 설치 (`express` or `@nestjs/core`, `cors`, `dotenv`, `bcryptjs`, `jsonwebtoken`, `multer`)
  - [ ] 개발 도구 설치 (`typescript`, `ts-node`, `nodemon`, `@types/...`)
- [ ] **데이터베이스 및 Prisma ORM 연동**
  - [ ] Prisma CLI 설치 및 초기화 (`npx prisma init`)
  - [ ] `.env` 파일에 PostgreSQL 연결 URI 설정 (`postgresql://spring:1q@W3e4r5t@localhost:5433/springdb`)
  - [ ] 기존 DB 테이블 자동 분석 및 스키마 생성 (`npx prisma db pull`)
  - [ ] Prisma Client 모듈 빌드 (`npx prisma generate`)
- [ ] **인증 (Auth) 및 보안 인프라 구축**
  - [ ] Bcrypt 기반 비밀번호 해시 검증 모듈 구현 (Spring Security BCrypt 호환 검사)
  - [ ] JWT 토큰 발급 및 서명 서비스 구현
  - [ ] Bearer 토큰 검증용 인증 미들웨어(Express) 또는 JWT Auth Guard(NestJS) 작성
  - [ ] 로그인 API 구현 (`POST /api/users/login`)
- [ ] **글로벌 예외 처리 및 공통 응답 설계**
  - [ ] API 에러 객체 표준화 및 전역 예외 필터(Spring ControllerAdvice 매핑) 구현

---

## 📅 Phase 2: 사용자 및 협업 코어 도메인 이식 (Auth & Collaboration)
- [ ] **User & Workspace 도메인**
  - [ ] 사용자 목록/조회/수정/삭제 API (`/users`)
  - [ ] 워크스페이스 생성 및 멤버십 매핑 API (`/workspaces`, `/workspaces/:id/members`)
  - [ ] 팀 및 팀원 관리 API (`/teams`, `/teams/:id/members`)
- [ ] **Project & Board 도메인**
  - [ ] 프로젝트 및 프로젝트 멤버십 API (`/projects`)
  - [ ] 보드 CRUD API (`/boards`)
  - [ ] 보드 컬럼 정의 및 JSONB 옵션 설정 API (`/board-columns`)
  - [ ] 보드 그룹(스프린트/섹션 구분) CRUD API (`/board-groups`)
  - [ ] 보드 뷰 (칸반/타임라인 등 뷰 설정) API (`/board-views`)
- [ ] **Item & EAV Value 도메인**
  - [ ] 업무 아이템(태스크 & 서브태스크 계층) CRUD API (`/items`)
  - [ ] 아이템 컬럼 값 저장(EAV 패턴) API 및 JSONB 다중 데이터타입 파싱 구현 (`/item-values`)
  - [ ] 드래그 앤 드롭 아이템 순서/그룹 변경에 따른 벌크 PUT API 연동
- [ ] **Comment & Mentions 도메인**
  - [ ] 아이템 댓글 및 스레드(답글) CRUD API (`/comments`)
  - [ ] 댓글 내 `@유저` 멘션 및 멘션 테이블 적재 API (`/comments/mentions`)

---

## 📅 Phase 3: 부가 기능 및 시스템 애드온 이식 (Add-ons & System)
- [ ] **파일 첨부 및 업로드 (Attachment & Files)**
  - [ ] Multer 기반 로컬 파일 업로드 핸들러 연동 (`POST /api/attachments/upload`)
  - [ ] 로컬 업로드 디렉토리(`uploads/`) 생성 및 Express 정적 서빙 미들웨어 설정
  - [ ] 첨부파일 메타데이터 DB 저장 API 구현 (`/attachments`)
- [ ] **활동 감사 로그 (Activity Logs)**
  - [ ] 아이템 생성/수정/삭제, 상태 변경 시 로그 적재 미들웨어/인터셉터 구현
  - [ ] 아이템 단위 히스토리 로그 조회 API (`GET /api/activity-logs/item/:itemId`)
- [ ] **인앱 알림 (Notifications)**
  - [ ] 멘션/담당자 지정/일정 임박 등에 따른 알림 적재 및 읽음 처리 API (`/notifications`)
- [ ] **대시보드 및 위젯 (Dashboards & Widgets)**
  - [ ] 대시보드 메타 정보 CRUD 및 외부 공유 토큰 처리 API (`/dashboards`)
  - [ ] 대시보드 위젯(배터리, 차트, 테이블 등) 그리드 배치 API (`/dashboard-widgets`)
- [ ] **자동화 규칙 빌더 (Automations)**
  - [ ] 보드별 트리거(상태 변경 등) 및 액션(Slack 알림, 상태 전이 등) 규칙 CRUD API (`/automations`)

---

## 📅 Phase 4: ERP 통합 서브시스템 이식 (ERP Subsystems)
- [ ] **인사/부서 관리 (HR - Department & Employee)**
  - [ ] 부서 트리 구조 조회 및 CRUD API (`/erp/hr/departments`)
  - [ ] 사원 번호 자동 채번 및 직원 정보 CRUD API (`/erp/hr/employees`)
- [ ] **근태 관리 (HR - Attendance)**
  - [ ] 출근(Check-in) 및 퇴근(Check-out) 기록 API (`/erp/hr/attendance/check-in`, `/check-out`)
  - [ ] 일별/월별 근태 이력 및 초과근무 시간 계산 API
- [ ] **연차 및 휴가 관리 (HR - Leave)**
  - [ ] 연차 신청 및 연간 잔여 연차 일수 계산 API (`/erp/hr/leaves`)
  - [ ] 연차 승인 시 PM 보드에 자동으로 업무 아이템(태스크)을 동기화 생성하는 연계 트리거 구현
- [ ] **급여 및 인사 평가 (HR - Payroll & Review)**
  - [ ] 월별 급여 일괄 생성, 확정 및 명세서 조회 API (`/erp/hr/payrolls`)
  - [ ] 다차원 인사평가 코멘트 및 점수 등록 API (`/erp/hr/reviews`)
- [ ] **영업 관리 (Sales)**
  - [ ] 견적서(Quotation) 작성 및 조회 API (`/erp/sales/quotations`)
  - [ ] 세금계산서(Tax Invoice) 발행 상태 및 상세 API (`/erp/sales/tax-invoices`)

---

## 📅 Phase 5: 프론트엔드 연동 및 통합 QA (Verification)
- [ ] **서버 구동 프로필 설정**
  - [ ] Node.js 개발 서버 실행 스크립트 등록 (`npm run dev`)
  - [ ] 포트 `9090`, contextPath `/api` 기본 연동 체크
- [ ] **프론트엔드 API 통신 검증**
  - [ ] Vue 3 프론트엔드(`localhost:5173`) 로컬 가동
  - [ ] 로그인 페이지 진입 및 로그인을 통한 JWT 헤더 삽입 검증
  - [ ] 사이드바 워크스페이스-프로젝트-보드 지연 로딩 렌더링 확인
  - [ ] 칸반/테이블 보드 뷰의 D&D 변경 내용이 DB에 무오류 반영되는지 확인
  - [ ] 파일 업로드 및 이미지 아바타 로딩이 온전하게 작동하는지 검증
