# Blog Backend

개인 블로그를 실제로 운영하기 위해 만든 Kotlin/Spring Boot REST API입니다. 정적 블로그에서 처리하기
어려운 콘텐츠 관리, 계층형 카테고리와 댓글, JWT 인증, 이미지 업로드, 조회 통계, 관리자 대시보드와
Maia 기반 체스 대국을 하나의 백엔드로 제공합니다.

- 운영 서비스: [blog.wypark.me](https://blog.wypark.me)
- 기본 API 주소: `http://localhost:8080`
- MinIO 콘솔: `http://localhost:9001`

## 주요 기능

- 게시글 작성·수정·삭제, 슬러그 기반 상세 조회와 검색
- 계층형 카테고리와 태그 필터링
- 회원·비회원 댓글 및 대댓글
- 이메일 인증, JWT Access/Refresh Token, Refresh Token Rotation
- S3 호환 이미지 저장소와 사용하지 않는 이미지 정리
- 일별 조회수 집계, 인기·상승·장기 미수정 게시글 분석
- 관리자 전용 게시글·카테고리·댓글·프로필·대시보드 API
- 일자별로 순환하는 오늘의 체스 퍼즐
- Maia3 난이도·모델 선택, 대국 저장, 기권·무르기와 PGN 조회를 지원하는 체스 게임

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language | Java 21, Kotlin 1.9.25 |
| Framework | Spring Boot 3.5.9, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, QueryDSL, JDBC, Flyway |
| Storage | PostgreSQL 17, Redis 7, S3/MinIO |
| Authentication | JWT, BCrypt, Redis Refresh Token Store |
| Chess Engine | Python 3.11, FastAPI, python-chess, Maia3 |
| Test | JUnit 5, MockMvc, H2 PostgreSQL compatibility mode |
| Runtime | Gradle Wrapper, Docker, Docker Compose |

## 아키텍처

```text
src/main/kotlin/me/wypark/blogbackend
├── api              HTTP 요청/응답과 컨트롤러
├── application      유스케이스, 입출력 모델, 외부 시스템 포트
├── domain           엔티티, 도메인 규칙, 저장소 계약
├── infrastructure   Redis, S3, JDBC, Security 어댑터
└── core             Spring 설정, JWT, 공통 예외 처리

maia-engine           Maia3 추론을 제공하는 독립 FastAPI 서비스
```

핵심 의존성 방향은 `api → application → domain`입니다. 애플리케이션 계층은 Redis나 S3 같은 구현체를
직접 참조하지 않고 포트 인터페이스에 의존합니다. 이 규칙은
[`LayerDependencyTest`](src/test/kotlin/me/wypark/blogbackend/architecture/LayerDependencyTest.kt)에서 자동으로
검증합니다.

## 빠른 시작

### 1. 사전 준비

- JDK 21
- Docker 및 Docker Compose
- 이메일 가입 기능을 사용할 경우 SMTP 계정과 앱 비밀번호

저장소는 Gradle Wrapper를 포함하므로 별도의 Gradle 설치는 필요하지 않습니다.

저장소를 받은 뒤 프로젝트 루트에서 환경 파일을 준비합니다.

```bash
cd blog-backend
cp .env.example .env
```

`.env`의 `JWT_SECRET`에는 최소 32바이트 길이의 Base64 키를 사용합니다.

```bash
openssl rand -base64 32
```

생성된 값을 `.env`의 `JWT_SECRET`에 넣고 DB, 메일, MinIO 값을 원하는 환경에 맞게 변경합니다.

### 2. 빈 DB에서 로컬 실행

현재 운영 프로필은 기존 운영 스키마를 검증하도록 `ddl-auto: validate`와 Flyway를 사용합니다. 빈 DB에서
기능을 확인할 때는 먼저 PostgreSQL·Redis·MinIO·Maia 엔진을 실행하고, 로컬 컨테이너에 한해 Hibernate가
스키마를 초기화하도록 실행합니다.

```bash
docker compose build blog-api maia-engine
docker compose up -d db redis minio maia-engine
docker compose run --rm --service-ports \
  -e SPRING_FLYWAY_ENABLED=false \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  blog-api
```

이 실행 방식은 로컬의 빈 DB를 빠르게 준비하기 위한 용도입니다. 운영 환경에서는 Flyway를 끄거나
`ddl-auto=update`를 사용하지 않습니다. Maia 엔진은 첫 실행 시 모델 파일을 내려받아
`maia_model_cache` 볼륨에 보관하므로 준비 시간이 더 걸릴 수 있습니다.

애플리케이션이 시작되면 공개 API로 상태를 확인합니다.

```bash
curl http://localhost:8080/api/profile
curl http://localhost:8080/api/posts
curl 'http://localhost:8080/api/chess-puzzles/today?timezone=Asia/Seoul'
```

중지는 실행 중인 애플리케이션에서 `Ctrl+C`를 누른 뒤 다음 명령을 사용합니다.

```bash
docker compose down
```

데이터까지 완전히 초기화하려면 `postgres_data/`, `minio_data/` 디렉터리를 별도로 삭제해야 합니다.

### 3. 기존 스키마를 사용하는 실행

운영 DB처럼 이미 스키마와 Flyway 이력이 준비된 환경은 기본 구성으로 실행할 수 있습니다.

```bash
docker compose up -d --build
docker compose logs -f blog-api
```

## 테스트와 빌드

```bash
./gradlew test
./gradlew clean check bootJar
```

테스트는 H2 인메모리 DB를 PostgreSQL 호환 모드로 사용하므로 PostgreSQL, Redis, MinIO를 별도로 실행하지
않아도 됩니다. 실행 JAR은 `build/libs/blog-backend-0.0.1-SNAPSHOT.jar`에 생성됩니다.

시스템 기본 JDK가 21이 아니라면 명시적으로 지정합니다.

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean check
```

## 인증 및 관리자 계정 준비

### 회원가입과 이메일 인증

유효한 SMTP 설정이 준비된 상태에서 회원가입합니다.

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@example.com",
    "password": "password123",
    "nickname": "admin"
  }'
```

메일로 받은 6자리 코드를 인증합니다.

```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@example.com",
    "code": "123456"
  }'
```

### 관리자 권한 부여

신규 가입자는 `ROLE_USER`로 생성됩니다. 로컬 블로그 운영 계정은 DB 콘솔에서 관리자 권한을 부여합니다.

```bash
docker compose exec db sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```sql
UPDATE member
SET role = 'ROLE_ADMIN', is_verified = TRUE
WHERE email = 'admin@example.com';
```

로그인 후 응답의 `accessToken`을 관리자 API에 사용합니다.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@example.com",
    "password": "password123"
  }'
```

```bash
curl -X POST http://localhost:8080/api/admin/posts \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <access-token>' \
  -d '{
    "title": "첫 번째 글",
    "content": "# Hello Blog",
    "slug": "hello-blog",
    "categoryId": null,
    "tags": ["Kotlin", "Spring"]
  }'
```

## API 요약

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 공개 | 회원가입 및 인증 메일 발송 |
| `POST` | `/api/auth/verify` | 공개 | 이메일 인증 |
| `POST` | `/api/auth/login` | 공개 | Access/Refresh Token 발급 |
| `POST` | `/api/auth/reissue` | 공개 | 토큰 재발급 및 Rotation |
| `POST` | `/api/auth/logout` | 로그인 | Refresh Token 삭제 |
| `GET` | `/api/posts` | 공개 | 게시글 목록·검색 |
| `GET` | `/api/posts/{slug}` | 공개 | 상세 조회 및 조회수 증가 |
| `GET` | `/api/categories` | 공개 | 카테고리 트리 조회 |
| `GET` | `/api/comments?postSlug=...` | 공개 | 댓글 트리 조회 |
| `POST` | `/api/comments` | 공개 | 회원·비회원 댓글 작성 |
| `DELETE` | `/api/comments/{id}` | 공개 | 작성자 또는 비밀번호 검증 후 삭제 |
| `GET` | `/api/profile` | 공개 | 블로그 프로필 조회 |
| `GET` | `/api/chess-puzzles/today` | 공개 | 오늘의 체스 퍼즐 |
| `POST` | `/api/chess/games` | 로그인 | Maia 체스 게임 생성 |
| `GET` | `/api/chess/games` | 로그인 | 내 체스 게임 목록 조회 |
| `GET` | `/api/chess/games/stats` | 로그인 | 내 체스 게임 전적 조회 |
| `GET` | `/api/chess/games/{gameId}` | 로그인 | 진행 중이거나 저장된 게임 조회 |
| `GET` | `/api/chess/games/{gameId}/pgn` | 로그인 | 게임 PGN 조회 |
| `POST` | `/api/chess/games/{gameId}/moves` | 로그인 | UCI 형식의 수를 두고 Maia 응답 요청 |
| `POST` | `/api/chess/games/{gameId}/resign` | 로그인 | 게임 기권 |
| `POST` | `/api/chess/games/{gameId}/undo` | 로그인 | 마지막 사용자 수와 Maia 응답 무르기 |
| `GET` | `/api/admin/dashboard` | 관리자 | 통계·액션 아이템 조회 |
| `POST/PUT/DELETE` | `/api/admin/posts/**` | 관리자 | 게시글 관리 |
| `POST/PUT/DELETE` | `/api/admin/categories/**` | 관리자 | 카테고리 관리 |
| `GET/DELETE` | `/api/admin/comments/**` | 관리자 | 댓글 관리 |
| `POST` | `/api/admin/images` | 관리자 | 이미지 업로드 |
| `PUT` | `/api/admin/profile` | 관리자 | 프로필 수정 |

게시글 목록은 `keyword`, `category`, `tag`, `page`, `size`, `sort` 쿼리 파라미터를 지원합니다. 대시보드는
`range=7d|30d|90d`와 IANA timezone 값을 받습니다.

## 공통 응답과 오류

모든 API는 같은 응답 형태를 사용합니다.

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

인증 실패는 `401 UNAUTHORIZED`, 관리자 권한 부족은 `403 FORBIDDEN`을 반환합니다. 입력 검증 및 비즈니스
규칙 위반은 같은 응답 구조의 오류 메시지로 반환되며, 예상하지 못한 예외의 내부 내용은 클라이언트에
노출하지 않습니다.

## 환경 변수

| 변수 | 필수 환경 | 설명 |
| --- | --- | --- |
| `DB_USER`, `DB_PASS` | Docker Compose | 로컬 PostgreSQL 계정 |
| `SPRING_DATASOURCE_URL` | 운영 | `host:port/database` 형식의 PostgreSQL 주소 |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | 운영 | PostgreSQL 인증 정보 |
| `JWT_SECRET` | 전체 | Base64 인코딩된 JWT HMAC 키 |
| `MAIL_USER`, `MAIL_PASS` | Docker Compose | SMTP 계정과 앱 비밀번호 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | 운영 | SMTP 인증 정보 |
| `REDIS_HOST` | 운영 | Redis 호스트 |
| `MAIA_ENGINE_URL` | 운영 | Maia FastAPI 서비스 주소, 기본값 `http://localhost:8000` |
| `MAIA_GAME_SESSION_TTL` | 운영 | Redis 게임 세션 TTL, 기본값 `PT6H` |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | 운영 | S3 호환 저장소 인증 정보 |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | 로컬 | MinIO 관리자 계정 |

운영 설정은 [`application-prod.yml`](src/main/resources/application-prod.yml), 테스트 설정은
[`application-test.yml`](src/test/resources/application-test.yml)에 있습니다. CI/CD와 서버 배포 흐름은
[`docs/ci-cd.md`](docs/ci-cd.md)를 참고합니다.

## 개발 규칙

구조, 테스트 범위, 데이터베이스 변경과 외부 동작 호환성 규칙은 [`AGENTS.md`](AGENTS.md)에 정리되어
있습니다. 변경 전 해당 문서를 먼저 읽고, 최소한 `./gradlew test`를 통과시켜야 합니다.
