# 1단계: 빌드 스테이지
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# Gradle 빌드 (실행 가능한 bootJar만 생성하도록 함)
RUN ./gradlew clean bootJar -x test

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안을 위해 비루트 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# 중요: 파일을 복사할 때 소유권을 spring 사용자에게 부여합니다.
# 경로를 /app/app.jar로 명시하거나, WORKDIR 내의 상대 경로를 사용합니다.
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# 사용자 전환
USER spring:spring

# 실행 경로 수정: /app.jar가 아니라 app.jar (현재 위치) 또는 /app/app.jar를 사용해야 합니다.
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]