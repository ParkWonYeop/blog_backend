# CI/CD 운영 메모

이 저장소는 Gitea Actions 기준으로 `main` 브랜치에 push되면 백엔드와 Maia3 체스 엔진 브리지를 함께 자동 배포하도록 구성되어 있다.

## 현재 운영 방식

- 백엔드 서비스: `blog-api.service`
- 백엔드 실행 디렉터리: `/root/blog-backend`
- 백엔드 실행 파일: `/root/blog-backend/blog-backend.jar`
- 백엔드 실행 방식: `java -jar -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod blog-backend.jar`
- Maia 엔진 서비스: `maia-engine.service`
- Maia 엔진 실행 디렉터리: `/root/blog-backend/maia-engine`
- Maia 엔진 venv: `/root/blog-backend/maia-engine-venv`
- Maia 엔진 URL: `http://127.0.0.1:8000`

백엔드는 기본값으로 `MAIA_ENGINE_URL=http://localhost:8000`을 사용하므로, Maia 엔진을 같은 서버에서 띄우면 별도 환경변수 없이 연동된다.

## 배포 흐름

1. `main` 브랜치 push 또는 수동 실행(`workflow_dispatch`)
2. Java 21로 `./gradlew test --no-daemon` 실행
3. Python 3.11로 `maia-engine/app/main.py` 문법 검사
4. 테스트 성공 시 `./gradlew clean bootJar -x test --no-daemon`으로 Spring Boot jar 빌드
5. `maia-engine` 디렉터리를 tar로 패키징
6. jar, Maia 엔진 tar, `deploy/systemd/maia-engine.service`를 SSH/SCP로 서버에 업로드
7. 서버에서 Maia 엔진 코드를 `/root/blog-backend/maia-engine`으로 교체
8. `/root/blog-backend/maia-engine-venv`를 생성하거나 재사용
9. venv가 깨져 있거나 pip가 없으면 삭제 후 재생성
10. `requirements.txt` 해시가 바뀌었으면 Python 의존성 재설치
11. `maia-engine.service` 설치, `systemctl daemon-reload`, enable, restart
12. `http://127.0.0.1:8000/health` 헬스체크
13. Spring Boot jar를 `/root/blog-backend/blog-backend.jar`로 교체
14. `blog-api.service` 재시작 후 active 상태 확인

Maia 엔진 배포 실패 시 이전 Maia 코드 디렉터리로 롤백을 시도한다. 백엔드 jar 재시작 실패 시 직전 jar 백업으로 롤백을 시도한다.

## Gitea Actions 준비

- Repository Settings에서 Actions가 활성화되어 있어야 한다.
- Gitea Runner가 `ubuntu-latest` label을 처리할 수 있어야 한다.
- `actions/checkout`, `actions/setup-java`, `actions/setup-python` 액션을 사용할 수 있어야 한다.

## Repository Secrets

Gitea repository secrets에 다음 값을 등록한다.

| 이름 | 설명 |
| --- | --- |
| `DEPLOY_HOST` | 배포 서버 호스트 또는 IP |
| `DEPLOY_PORT` | SSH 포트. 비우면 22 |
| `DEPLOY_USER` | 배포 서버 SSH 사용자 |
| `DEPLOY_KEY` | 배포 서버 접속용 private key |

## 서버 준비 사항

서버에는 다음이 필요하다.

- Java 21
- systemd
- Python 3.11 또는 Python 3
- Python venv 모듈(`python3-venv` 계열 패키지). 없으면 배포 스크립트가 `apt-get` 또는 `sudo apt-get`으로 자동 설치를 시도한다.
- `git`
- `sha256sum`
- `tar`
- 외부 네트워크 접근. `requirements.txt`가 `git+https://github.com/CSSLab/maia3.git`를 설치한다.
- `DEPLOY_USER`가 `/root/blog-backend`에 쓸 수 있고 `/etc/systemd/system`에 서비스 파일을 설치하며 `systemctl`을 실행할 수 있어야 한다.
- Python venv 패키지가 없는 서버에서는 `DEPLOY_USER`가 `apt-get install`을 실행할 수 있어야 한다. root 접속이 아니면 passwordless sudo 권한이 필요하다.

첫 설치 또는 Maia3 의존성 변경 시 Python 패키지 설치와 모델 캐시 준비 때문에 배포가 오래 걸릴 수 있다. 모델 캐시는 `/root/blog-backend/.cache/huggingface`에 저장된다.

## 체스 배포 확인

배포 후 서버에서 다음을 확인할 수 있다.

```bash
systemctl status maia-engine.service
curl -fsS http://127.0.0.1:8000/health
systemctl status blog-api.service
```

백엔드 체스 API는 로그인한 사용자만 사용할 수 있다. 퍼즐 API(`/api/chess-puzzles/**`)는 비로그인 접근이 유지된다.

## Docker Compose

`docker-compose.yml`에도 Maia 엔진 서비스가 정의되어 있지만, 현재 CI/CD 경로는 Docker Compose가 아니라 systemd 기반 jar + Maia systemd 서비스 배포를 사용한다.
