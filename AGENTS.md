# AGENTS.md

이 문서는 저장소 전체에 적용되는 개발 지침이다. 사람과 코딩 에이전트 모두 변경 전에 이 문서를 읽고,
현재 동작과 계층 경계를 유지해야 한다. 더 하위 디렉터리에 별도의 `AGENTS.md`가 추가되면 그 문서가 해당
범위에서 우선한다.

## 프로젝트 기준

- 언어와 런타임: Kotlin 1.9.25, Java 21
- 프레임워크: Spring Boot 3.5.9
- 체스 엔진: Python 3.11, FastAPI, Maia3
- 빌드: Gradle Wrapper만 사용한다.
- 기본 패키지: `me.wypark.blogbackend`
- API 기본 응답: `ApiResponse<T>`
- 운영 프로필: `prod`
- 테스트 프로필: `test`

시스템 기본 JDK 버전을 신뢰하지 말고 Java 21인지 먼저 확인한다.

```bash
java -version
./gradlew test
```

필요하면 다음처럼 Java 21을 명시한다.

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew clean check
```

## 반드시 지켜야 할 원칙

1. 요청받지 않은 API 동작 변경을 하지 않는다.
2. 기존 URL, HTTP 메서드, 응답 필드, 메시지, 상태 코드, 정렬과 페이징 의미를 호환성 계약으로 취급한다.
3. `api → application → domain` 의존성 방향을 거슬러 참조하지 않는다.
4. `application`은 `infrastructure` 구현 클래스를 직접 참조하지 않고 포트 인터페이스에 의존한다.
5. JPA 엔티티를 컨트롤러에서 직접 반환하지 않는다.
6. 외부 시스템 호출 실패를 `printStackTrace`로 처리하거나 조용히 삼키지 않는다.
7. 생성된 파일, 빌드 산출물, 로컬 데이터 디렉터리는 수정하거나 커밋하지 않는다.

계층 위반은 `LayerDependencyTest`에서 검사한다. 테스트를 통과시키기 위해 규칙을 약화하지 말고 잘못된
의존성을 올바른 계층으로 이동한다.

## 패키지 배치 기준

| 패키지 | 책임 | 포함해도 되는 것 | 포함하면 안 되는 것 |
| --- | --- | --- | --- |
| `api` | HTTP 전달 계층 | Controller, HTTP 전용 모델, `ApiResponse` | JPA 쿼리, 외부 저장소 구현, 도메인 상태 변경 |
| `application` | 유스케이스 조정 | Service, command/response model, port interface | Redis/S3/JDBC 구현 세부사항 |
| `domain` | 핵심 모델과 규칙 | Entity, 도메인 메서드, repository contract, read model | Controller, API DTO, 인프라 어댑터 |
| `infrastructure` | 외부 기술 연동 | Redis/S3/JDBC/Security 구현체 | HTTP 응답 조립, 비즈니스 정책 결정 |
| `core` | 횡단 관심사 | Configuration, JWT adapter, global error handling | 기능별 유스케이스 |
| `maia-engine` | 체스 추론 프로세스 | FastAPI endpoint, Maia/python-chess 연동 | 블로그 DB 접근, 인증·HTTP 응답 정책 |

새 기능은 먼저 어느 계층의 책임인지 정한 후 파일을 만든다. 작은 기능에 불필요한 추상화를 추가하지
않되, 네트워크·스토리지·시간처럼 교체하거나 테스트해야 하는 경계는 포트로 분리한다.

## 작업 절차

1. `git status --short`로 기존 사용자 변경을 확인한다.
2. 관련 Controller, application service, domain model, adapter와 테스트를 함께 읽는다.
3. 변경 전 현재 테스트 기준선을 확보한다.
4. 외부 동작을 유지하는 가장 작은 단위로 구현한다.
5. 변경한 책임에 맞는 단위 또는 통합 테스트를 추가한다.
6. `git diff --check`와 전체 테스트를 실행한다.
7. 변경 파일, 검증 결과, 남은 위험을 명확히 보고한다.

사용자의 변경을 덮어쓰거나 관련 없는 파일을 정리하지 않는다. `git reset --hard`, 강제 checkout, 임의의
파일 삭제 같은 파괴적 명령은 명시적인 요청 없이는 사용하지 않는다.

## Kotlin 코드 스타일

- 와일드카드 import를 사용하지 않는다.
- 생성자 주입을 사용하고 필드 주입을 사용하지 않는다.
- `!!` 대신 `requireNotNull`, nullable 흐름, 명시적인 비즈니스 예외를 사용한다.
- 함수와 클래스 이름으로 드러나는 내용을 장문의 주석으로 반복하지 않는다.
- 주석은 제약의 이유, 호환성 배경, 비직관적인 선택을 설명할 때만 남긴다.
- 범용 `Exception` catch는 외부 시스템 경계의 fail-safe 처리처럼 이유가 분명할 때만 사용하고 로그를 남긴다.
- 시간에 의존하는 코드는 `LocalDate.now()`나 `Date()`를 직접 호출하기보다 주입된 `Clock`을 사용한다.
- 설정은 흩어진 `@Value` 대신 `@ConfigurationProperties`로 묶는다.
- 의미 있는 상수는 companion object 또는 전용 값 객체로 이름을 부여한다.
- 한 파일이 여러 기능의 책임을 갖기 시작하면 mapper, selector, parser, port 등 응집된 단위로 분리한다.

`.editorconfig`의 UTF-8, LF, 4칸 들여쓰기와 120자 줄 길이를 따른다.

## Maia 엔진

- `maia-engine`은 Spring 애플리케이션과 HTTP/JSON으로 통신하는 독립 프로세스로 유지한다.
- Spring 쪽 요청·응답 계약은 `application` 포트에, `RestClient` 구현은 `infrastructure`에 둔다.
- 엔진의 `/health`, `/maia/state`, `/maia/move` 계약을 변경하면 Kotlin 모델과 호출부를 함께 수정한다.
- 모델명, 디바이스, 캐시 경로는 환경 변수로 주입하고 모델 파일을 저장소에 커밋하지 않는다.
- Python 예외의 내부 경로나 모델 세부 정보를 API 사용자에게 그대로 노출하지 않는다.

## 도메인과 JPA

- 엔티티 상태는 public setter보다 의도가 드러나는 도메인 메서드로 변경한다.
- Kotlin JPA `all-open`과 Hibernate 프록시 호환을 위해 엔티티 setter는 필요 시 `protected set`을 사용한다.
- 양방향 연관관계는 한 메서드에서 양쪽을 함께 갱신한다.
- `FetchType.LAZY`를 기본으로 유지하고, 필요한 조회만 fetch join·projection·batch 전략으로 최적화한다.
- OSIV가 꺼져 있으므로 DTO 변환에 필요한 lazy 관계는 트랜잭션 안에서 읽어야 한다.
- 영속화된 ID는 `requireNotNull(entity.id)`처럼 불변식을 명시한다.
- 대량 갱신, 원자적 증가, 통계 쿼리처럼 JPA보다 SQL이 명확한 경우에만 JDBC를 사용한다.
- QueryDSL 목록 조회는 엔티티 전체보다 필요한 필드의 read model projection을 우선한다.

데이터베이스 구조를 바꾸면 기존 migration을 수정하지 말고 `src/main/resources/db/migration`에 새로운
Flyway migration을 추가한다. PostgreSQL에서 먼저 성립하는 SQL을 작성하고 H2 호환 테스트가 필요한지
별도로 판단한다. 운영 스키마에 대한 파괴적 DDL은 명시적인 승인 없이 추가하지 않는다.

## 애플리케이션 서비스

- 클래스 수준은 `@Transactional(readOnly = true)`를 기본으로 하고 쓰기 유스케이스에만 `@Transactional`을 붙인다.
- 서비스는 유스케이스를 조정하고, 파싱·선택·매핑 같은 순수 로직은 별도 컴포넌트로 분리한다.
- Redis, S3, 메일, 통계 JDBC 같은 외부 구현은 application port 뒤에 둔다.
- 사용자에게 전달할 수 있는 규칙 위반은 `BusinessException`으로 표현한다.
- 스토리지 파일 삭제와 DB 트랜잭션처럼 원자성이 다른 작업을 추가할 때 실패 순서와 보상 전략을 검토한다.

## API와 보안

- 성공과 실패 모두 `ApiResponse` 구조를 유지한다.
- 요청 입력은 Controller 경계에서 Bean Validation으로 검증한다.
- 관리자 API는 `/api/admin/**` 아래에 두고 `ROLE_ADMIN` 정책을 유지한다.
- 공개 API를 추가하거나 관리자 경로를 변경하면 `SecurityConfig`와 보안 테스트를 함께 수정한다.
- Access Token은 `Authorization: Bearer <token>` 헤더에서만 읽는다.
- 비밀번호, JWT, 인증 코드, SMTP/S3 자격 증명을 로그나 소스에 남기지 않는다.
- Secret은 환경 변수로 주입하고 실제 값이 든 `.env`를 커밋하지 않는다.
- 인증 오류의 내부 예외나 스택 트레이스를 API 응답에 노출하지 않는다.

## 테스트 기준

변경 성격에 따라 다음 테스트를 추가한다.

- 순수 계산·파싱·선택 로직: 빠른 단위 테스트
- 엔티티 상태와 연관관계: domain 단위 테스트
- QueryDSL/JPA/JDBC 쿼리: H2 또는 PostgreSQL 통합 테스트
- Security, HTTP 상태, JSON 계약: MockMvc 통합 테스트
- 계층 이동이나 새 패키지: `LayerDependencyTest` 확인
- 시간 기반 기능: 고정 또는 가변 `Clock` 사용

최소 검증:

```bash
./gradlew test
git diff --check
```

배포·설정·의존성·패키지 구조에 영향을 주는 변경의 최종 검증:

