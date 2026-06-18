# FastAPI 백엔드 실행 방법

이 문서는 `fastapibackend` 프로젝트를 로컬 환경에서 실행하기 위한 가이드입니다.

## 1. 프로젝트 디렉토리로 이동

터미널을 열고 `fastapibackend` 디렉토리로 이동합니다.

```bash
cd /home/knh11/spring/boot/fastapibackend
```

## 2. 가상 환경(Virtual Environment) 활성화

프로젝트 내에 이미 설정된 가상 환경(`venv`)을 활성화합니다.

```bash
source venv/bin/activate
```
*활성화가 완료되면 터미널 프롬프트 앞에 `(venv)`가 표시됩니다.*

## 3. 패키지 설치 (필요시)

의존성 패키지가 아직 설치되지 않았거나 업데이트가 필요한 경우, `requirements.txt`를 통해 패키지를 설치합니다.

```bash
pip install -r requirements.txt
```

## 4. 환경 변수 설정 (필요시)

디렉토리 내에 있는 `.env` 파일을 확인하여 데이터베이스 연결 정보 등이 올바르게 설정되어 있는지 확인합니다.

## 5. FastAPI 서버 실행

`uvicorn`을 사용하여 FastAPI 애플리케이션을 실행합니다. `--reload` 옵션을 추가하면 코드가 변경될 때마다 서버가 자동으로 재시작됩니다.

```bash
uvicorn app.main:app --reload
```

## 6. 실행 확인

서버가 정상적으로 실행되면 터미널에 `Application startup complete.` 등의 메시지가 출력됩니다.

* **API 기본 접속:** [http://127.0.0.1:8000](http://127.0.0.1:8000)
* **Swagger UI API 문서 (추천):** [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
* **ReDoc API 문서:** [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)

## 가상 환경 비활성화

작업을 마치고 가상 환경을 종료하려면 다음 명령어를 입력하세요.

```bash
deactivate
```
