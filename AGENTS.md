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

