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

