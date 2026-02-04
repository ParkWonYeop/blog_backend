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

