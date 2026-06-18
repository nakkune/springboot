# FastAPI Backend 설치 및 실행 가이드 (FastAPI Backend Installation & Run Guide)

이 문서는 다른 PC에서 Git 저장소의 소스코드를 클론(Clone)하여 `fastapibackend` 프로젝트를 환경설정하고 실행하는 가이드를 제공합니다.

---

## 1. 사전 요구사항 (Prerequisites)

프로젝트를 실행하려면 설치하려는 PC에 아래 소프트웨어들이 미리 설치되어 있어야 합니다.
* **Python**: `3.12` 버전 이상 권장
* **PostgreSQL**: 버전 `15` 이상 권장 (포트: `5433` 또는 사용자 지정 포트)
* **Git**

---

## 2. 저장소 클론 및 폴더 이동 (Clone and Directory Change)

저장소를 클론한 후 `fastapibackend` 디렉토리로 이동합니다.

```bash
# 저장소 클론 (실제 Git 저장소 주소 입력)
git clone <repository_url>
cd spring/boot/fastapibackend
```

---

## 3. 가상환경 생성 및 패키지 설치 (Virtual Environment & Dependencies)

Python 가상환경을 생성하고 활성화한 뒤, 필요한 패키지를 설치합니다.

### Linux / macOS
```bash
# 1. 가상환경 생성 (venv)
python3 -m venv venv

# 2. 가상환경 활성화
source venv/bin/activate

# 3. 필수 패키지 설치
pip install --upgrade pip
pip install -r requirements.txt
```

### Windows (PowerShell / CMD)
```powershell
# 1. 가상환경 생성 (venv)
python -m venv venv

# 2. 가상환경 활성화 (PowerShell)
.\venv\Scripts\Activate.ps1
# 또는 CMD
# .\venv\Scripts\activate.bat

# 3. 필수 패키지 설치
pip install --upgrade pip
pip install -r requirements.txt
```

---

## 4. 환경 변수 설정 (Environment Variables)

`fastapibackend` 디렉토리 루트에 `.env` 파일을 생성하고, 대상 PC의 데이터베이스 연결 환경에 맞게 값을 수정합니다.

> **주의**: `.env` 파일은 민감한 정보를 담고 있어 Git 관리 대상에 따라 무시되거나 포함될 수 있으므로, 실행 전 로컬 환경 정보를 정확하게 명시해 주어야 합니다.

`.env` 파일 작성 내용 예시:
```env
PROJECT_NAME=FastAPI Backend
API_V1_STR=/api
SECRET_KEY=1q@W3e4r5t_secret_key_should_be_long_and_random
ACCESS_TOKEN_EXPIRE_MINUTES=11520

# PostgreSQL 데이터베이스 설정
POSTGRES_SERVER=127.0.0.1
POSTGRES_PORT=5433
POSTGRES_USER=spring
POSTGRES_PASSWORD=1q@W3e4r5t
POSTGRES_DB=springdb
```

---

## 5. 데이터베이스 생성 및 마이그레이션 (Database & Migration)

1. PostgreSQL 클라이언트를 사용하여 설정한 `.env` 정보에 부합하는 데이터베이스(`springdb`)를 생성합니다.
2. 가상환경이 활성화된 상태에서 아래 명령어를 실행하여 데이터베이스 스키마 및 마이그레이션을 적용합니다.

```bash
# Alembic 최신 버전 마이그레이션 적용
alembic upgrade head
```

---

## 6. 서버 실행 (Run the Application)

FastAPI 애플리케이션 서버를 실행합니다.

```bash
# FastAPI uvicorn 서버 실행 (포트: 9090)
uvicorn app.main:app --port 9090 --reload
```

* 실행 후 웹 브라우저에서 `http://localhost:9090/docs` 주소로 접속하면 Swagger UI가 노출되며 API 엔드포인트를 확인 및 테스트할 수 있습니다.

---

## 7. 테스트 코드 검증 (Run Tests)

프로젝트의 모든 유닛/통합 테스트가 제대로 작동하는지 확인하려면 아래 명령을 실행합니다.

```bash
# pytest 실행
pytest
```
