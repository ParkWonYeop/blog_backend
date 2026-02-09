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

