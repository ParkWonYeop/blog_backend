# CI/CD 운영 메모

이 저장소는 Gitea Actions 기준으로 `main` 브랜치에 push되면 자동 배포되도록 구성한다.
현재 운영 서버는 Docker Compose가 아니라 Java 21 + systemd 방식으로 실행 중이다.

## 현재 운영 방식

- 운영 서비스: `blog-api.service`
- 실행 디렉터리: `/root/blog-backend`
- 실행 파일: `/root/blog-backend/blog-backend.jar`
- 실행 방식: `java -jar -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod blog-backend.jar`
- 운영 환경 변수와 외부 DB/Redis/S3 설정은 systemd service에 등록되어 있다.

## 동작 흐름

1. `main` 브랜치 push 또는 수동 실행(`workflow_dispatch`)
2. Java 21로 `./gradlew test --no-daemon` 실행
3. 테스트 성공 시 `./gradlew clean bootJar -x test --no-daemon`으로 Spring Boot jar 빌드
4. 빌드된 jar를 SSH/SCP로 서버의 `/tmp/blog-backend.jar.new`에 업로드
5. 서버에서 기존 `/root/blog-backend/blog-backend.jar`를 timestamp 백업
6. 새 jar를 `/root/blog-backend/blog-backend.jar`로 교체
7. `systemctl restart blog-api.service` 실행 후 active 상태 확인
8. 재시작 실패 또는 비정상 종료 시 직전 jar로 rollback 시도

## Gitea Actions 준비

- Repository Settings에서 Actions를 활성화한다.
- Gitea Runner가 `ubuntu-latest` label을 처리할 수 있어야 한다.
- 현재 백엔드 서버에는 `blog-backend` runner가 `ubuntu-latest` label로 등록되어 있다.
- Docker는 runner job 실행에 쓰일 수 있지만, 애플리케이션 배포 방식은 systemd jar 교체 방식이다.

## Repository Secrets

Gitea repository secrets에 다음 값을 등록한다.

| 이름 | 설명 |
| --- | --- |
| `DEPLOY_HOST` | 배포 서버 호스트 또는 IP |
| `DEPLOY_PORT` | SSH 포트, 비우면 22 |
| `DEPLOY_USER` | 배포 서버 SSH 사용자 |
| `DEPLOY_KEY` | 배포 서버 접속용 private key |

## 서버 준비 사항

- 서버에는 Java 21이 설치되어 있어야 한다.
- `blog-api.service`가 `/root/blog-backend/blog-backend.jar`를 실행하도록 구성되어 있어야 한다.
- `DEPLOY_USER`는 `/root/blog-backend`에 jar를 쓸 수 있고 `systemctl restart blog-api.service`를 실행할 수 있어야 한다.
- 운영 DB는 배포 전에 백업하는 것을 권장한다. Flyway migration은 애플리케이션 시작 시 자동 적용된다.
- `docker-compose.yml`은 현재 CI/CD 배포 경로에서 사용하지 않는다.
