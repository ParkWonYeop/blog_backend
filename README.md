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
- 초대 코드·랜덤 매칭으로 시작하는 WebSocket 기반 온라인 체스 대국 (블리츠·래피드 시계, 끊김 90초 후 기권 처리)

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language | Java 21, Kotlin 1.9.25 |
| Framework | Spring Boot 3.5.16, Spring MVC, Spring Security |
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

## 관리자 MCP 연결

관리자 화면의 **MCP 연결**(`/admin/mcp`)에서 연결 키를 발급하면 Codex·Claude Code 등에서 블로그를
관리할 수 있습니다. 서버 주소는 `https://blogserver.wypark.me/api/admin/mcp`이며 로컬에서는
`http://localhost:8080/api/admin/mcp`를 사용합니다. 공식 MCP Java SDK 0.18.5의 stateless Streamable HTTP
전송을 사용하며 `2025-03-26`, `2025-06-18`, `2025-11-25` 프로토콜을 지원합니다.

### 연결 키

1. 이메일 인증을 마친 관리자 계정으로 로그인하고 **관리자 → MCP 연결**을 엽니다.
2. 앱 이름, 권한, 유효 기간(1–365일, 기본 90일)을 지정합니다. 기본 권한은 **읽기 전용**입니다.
3. 발급 직후 한 번 표시되는 키를 복사해 클라이언트 실행 환경의 `BLOG_MCP_TOKEN`에 보관합니다.
4. 사용을 끝낸 키는 같은 화면에서 폐기합니다. 만료·폐기되거나 발급자의 관리자 권한/이메일 인증이
   해제되면 다음 요청부터 거절됩니다.

키는 `Authorization: Bearer <연결 키>`로만 전달합니다. DB에는 SHA-256 해시와 표시용 접두사만 저장하며,
전체 키는 다시 조회할 수 없습니다. MCP 키로 일반 관리자 REST API나 다른 연결 키를 관리할 수 없습니다.
일반 로그인 JWT도 MCP 연결 키를 대신할 수 없습니다. 키 발급·목록·폐기는 기존 관리자 JWT를 사용합니다.

### Codex

Codex를 실행하는 환경에서 `BLOG_MCP_TOKEN`을 설정한 뒤 다음 명령으로 서버를 등록합니다.
실제 키를 명령이나 설정 파일에 직접 넣지 않습니다.

```bash
codex mcp add blog-admin \
  --url https://blogserver.wypark.me/api/admin/mcp \
  --bearer-token-env-var BLOG_MCP_TOKEN
```

설정 파일을 직접 편집한다면 `~/.codex/config.toml`에 다음 내용을 추가합니다.

```toml
[mcp_servers.blog-admin]
url = "https://blogserver.wypark.me/api/admin/mcp"
bearer_token_env_var = "BLOG_MCP_TOKEN"
```

데스크톱 앱도 자신의 실행 환경에서 해당 변수를 읽을 수 있어야 합니다.
[Codex 공식 MCP 문서](https://learn.chatgpt.com/docs/extend/mcp)를 참고하세요.

### Claude Code

프로젝트의 `.mcp.json`에 다음 서버 항목을 추가하고, `BLOG_MCP_TOKEN`이 설정된 환경에서 Claude Code를
시작합니다. `${BLOG_MCP_TOKEN}`은 아래처럼 문자 그대로 저장해 실행 시 환경 변수로 확장합니다.

```json
{
  "mcpServers": {
    "blog-admin": {
      "type": "http",
      "url": "https://blogserver.wypark.me/api/admin/mcp",
      "headers": { "Authorization": "Bearer ${BLOG_MCP_TOKEN}" }
    }
  }
}
```

`/mcp`에서 연결 상태를 확인합니다.
[Claude Code 공식 MCP 문서](https://code.claude.com/docs/en/mcp#environment-variable-expansion-in-mcp-json)
에 환경 변수 설정 방법이 나와 있습니다.

### 제공 도구와 동작

| 대상 | 읽기 도구 | 쓰기 도구 (`READ_WRITE` 필요) |
| --- | --- | --- |
| 게시글 | `blog_list_posts`, `blog_get_post` | `blog_create_post`, `blog_update_post`, `blog_delete_post` |
| 카테고리 | `blog_list_categories` | `blog_create_category`, `blog_update_category`, `blog_delete_category` |
| 댓글 | `blog_list_comments` | `blog_create_comment`, `blog_delete_comment` |
| 프로필 | `blog_get_profile` | `blog_update_profile` |

조회 목록은 0부터 시작하는 `page`와 최대 100인 `size`를 받습니다. 관리자 게시글 상세 조회는 조회수를
올리지 않으며 수정에 필요한 `categoryId`와 `tags`를 포함합니다. 작성은 즉시 공개되고 수정은 전달한
내용으로 교체됩니다. 기존 도구를 재사용하므로 글 삭제 시 댓글·이미지, 카테고리 삭제 시 하위 카테고리,
댓글 삭제 시 답글도 함께 처리됩니다. 본문·프로필에서 제거한 이미지는 기존 관리자 화면과 동일하게
삭제됩니다. 수정 전 상세 내용을 읽고 유지할 필드를 함께 전달하세요.

삭제 도구는 사용자의 승인 이후 `confirm: true`를 전달해야 실행됩니다. 이 인자는 클라이언트의 실제
사용자 승인 여부를 증명하지 않으므로 클라이언트의 도구 승인 설정도 유지하세요. 읽기 전용 권한은
도구 설명과 별개로 서버에서 강제하며, 쓰기 성공 로그에는 도구 이름과 키 ID만 기록합니다.
MCP 응답만 표준 JSON-RPC 형식을 사용하고 기존 REST API의 `ApiResponse` 계약은 유지합니다.

배포 시 Flyway가 `mcp_key` 테이블과 인덱스를 추가합니다. 새 서버 환경 변수는 필요 없습니다.
리버스 프록시는 MCP 경로의 POST, Authorization, Accept, MCP-Protocol-Version 헤더를 전달해야 합니다.
허용 Origin은 기존 `blog.cors.allowed-origins` 설정을 따릅니다. 브라우저 기반 클라이언트에서 필요한
Origin만 추가하세요. 이 연결은 Bearer 키를 지원하는 클라이언트용이며 OAuth 로그인은 제공하지 않습니다.
요청 빈도 제한은 `remoteAddr`를 기준으로 하므로, 운영 프록시는 외부에서 받은 `Forwarded`와
`X-Forwarded-For`를 신뢰하지 말고 실제 접속 IP로 덮어써야 합니다. API 서버도 신뢰하는 프록시만
접속할 수 있게 유지해야 합니다. 그렇지 않으면 제한을 우회하거나 여러 사용자가 한 제한을 공유할 수 있습니다.

[MCP SDK의 요청 크기 보안 수정](https://github.com/modelcontextprotocol/java-sdk/security/advisories/GHSA-4x8c-5vv7-973f)을
포함한 버전을 사용합니다. 이 프로젝트의 WebMVC 전송은 요청 본문을 문자열로 읽으므로, 애플리케이션에서도
인증 확인 후 `/api/`의 요청 본문을 최대 8MiB로 제한합니다. 길이를 알 수 없는 전송에도 같은 제한을
적용합니다. 이미지 멀티파트 업로드는 기존 10MB 설정을 사용합니다.

## API 요약

| Method | Endpoint | 인증 | 설명 |
| --- | --- | --- | --- |
| `POST` | `/api/admin/mcp` | MCP 연결 키 | MCP 초기화·도구 조회·호출 |
| `GET`, `POST` | `/api/admin/mcp/keys` | 관리자 JWT | 내 연결 키 목록·발급 |
| `DELETE` | `/api/admin/mcp/keys/{id}` | 관리자 JWT | 내 연결 키 폐기 |
| `POST` | `/api/auth/signup` | 공개 | 회원가입 및 인증 메일 발송 |
| `POST` | `/api/auth/verify` | 공개 | 이메일 인증 |
| `POST` | `/api/auth/login` | 공개 | Access Token 응답과 HttpOnly Refresh 쿠키 발급 |
| `POST` | `/api/auth/reissue` | 공개 | 쿠키 기반 토큰 재발급 및 Rotation |
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
| `GET` | `/api/chess/online/time-controls` | 로그인 | 온라인 대국 시간 설정 목록 |
| `GET` | `/api/chess/online/games` | 로그인 | 내 온라인 대국 목록 (진행 중 포함) |
| `GET` | `/api/chess/online/games/active` | 로그인 | 진행 중인 내 온라인 대국, 없으면 `data: null` |
| `GET` | `/api/chess/online/games/{gameId}` | 로그인 | 온라인 대국 상태 조회 (참가자만) |
| `WS` | `/ws/chess` | 로그인 | 온라인 대국 실시간 채널. 연결 후 `{"type":"AUTH","token":...}`로 인증하며 계정당 최신 연결 하나만 유지 |
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
| `REDIS_HOST`, `REDIS_PASSWORD` | 운영 | Redis 호스트와 인증 비밀번호 |
| `MAIA_ENGINE_URL` | 운영 | Maia FastAPI 서비스 주소, 기본값 `http://localhost:8000` |
| `MAIA_GAME_SESSION_TTL` | 운영 | Redis 게임 세션 TTL, 기본값 `PT6H` |
| `MAIA_MAX_ENGINES` | Maia 엔진 | 동시에 유지할 `maia3-uci` 프로세스 수, 기본값 `2` (모델별로 프로세스가 하나씩 뜨며 LRU로 퇴출) |
| `MAIA_ENGINE_IDLE_SECONDS` | Maia 엔진 | 이 시간 동안 쓰이지 않은 엔진 프로세스를 종료, 기본값 `900` |
| `MAIA_ENGINE_WAIT_SECONDS` | Maia 엔진 | 엔진이 바쁠 때 요청이 기다리는 최대 시간, 초과 시 503, 기본값 `30` |
| `OMP_NUM_THREADS` | Maia 엔진 | 추론에 쓸 CPU 스레드 수. systemd 유닛과 Compose는 `2`로 고정 |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | 운영 | S3 호환 저장소 인증 정보 |
| `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` | 로컬 | 필수 MinIO 관리자 계정(기본값 없음) |

운영 설정은 [`application-prod.yml`](src/main/resources/application-prod.yml), 테스트 설정은
[`application-test.yml`](src/test/resources/application-test.yml)에 있습니다. CI/CD와 서버 배포 흐름은
[`docs/ci-cd.md`](docs/ci-cd.md)를 참고합니다.

## 개발 규칙

구조, 테스트 범위, 데이터베이스 변경과 외부 동작 호환성 규칙은 [`AGENTS.md`](AGENTS.md)에 정리되어
있습니다. 변경 전 해당 문서를 먼저 읽고, 최소한 `./gradlew test`를 통과시켜야 합니다.
