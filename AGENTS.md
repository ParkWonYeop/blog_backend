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

