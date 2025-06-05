plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "me.wypark"
version = "0.0.1-SNAPSHOT"
description = "blog-backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val springCloudAwsVersion = "3.2.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    runtimeOnly("org.postgresql:postgresql")

    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:$springCloudAwsVersion")

    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")

    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val compileKotlinTask = tasks.named("compileKotlin")
val tempBuildDir = providers.provider {
    file(System.getenv("BLOG_BACKEND_ASCII_BUILD_DIR") ?: "${System.getenv("TEMP") ?: layout.buildDirectory.get().asFile.absolutePath}/blog-backend-build")
}
val javacKotlinClassesDir = providers.provider {
    tempBuildDir.get().resolve("kotlin-main-classes")
}

val syncKotlinClassesForJavac = tasks.register<Sync>("syncKotlinClassesForJavac") {
    dependsOn(compileKotlinTask)
    from(compileKotlinTask.map { it.outputs.files })
    into(javacKotlinClassesDir)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(syncKotlinClassesForJavac)
    classpath = files(javacKotlinClassesDir) + configurations.compileClasspath.get()
    options.isIncremental = false
}

val mainRuntimeClassesDir = providers.provider {
    tempBuildDir.get().resolve("main-runtime-classes")
}
val testRuntimeClassesDir = providers.provider {
    tempBuildDir.get().resolve("test-runtime-classes")
}

val syncMainClassesForTestRuntime = tasks.register<Sync>("syncMainClassesForTestRuntime") {
    dependsOn(tasks.named("classes"))
    from(layout.buildDirectory.dir("classes/kotlin/main"))
    from(layout.buildDirectory.dir("classes/java/main"))
    from(layout.buildDirectory.dir("resources/main"))
    into(mainRuntimeClassesDir)
}

val syncTestClassesForTestRuntime = tasks.register<Sync>("syncTestClassesForTestRuntime") {
    dependsOn(tasks.named("testClasses"))
    from(layout.buildDirectory.dir("classes/kotlin/test"))
    from(layout.buildDirectory.dir("classes/java/test"))
    from(layout.buildDirectory.dir("resources/test"))
    into(testRuntimeClassesDir)
}

tasks.withType<Test> {
    dependsOn(syncMainClassesForTestRuntime, syncTestClassesForTestRuntime)
    testClassesDirs = files(testRuntimeClassesDir)
    classpath = files(testRuntimeClassesDir, mainRuntimeClassesDir) + classpath
}
