package me.wypark.blogbackend

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

/**
 * [블로그 백엔드 애플리케이션 진입점]
 *
 * @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO):
 * Spring Data Web의 페이징 직렬화 방식을 설정합니다.
 * 최신 Spring Boot 버전에서는 PagedModel(구조체) 반환이 기본값이지만,
 * 기존 프론트엔드와의 호환성 및 명시적인 DTO 변환을 선호하여 'VIA_DTO' 모드를 채택했습니다.
 * 이는 내부 엔티티 구조가 외부 API 스펙에 직접 노출되는 것을 방지합니다.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class BlogBackendApplication

fun main(args: Array<String>) {
    /**
     * [환경 변수 로드 전략: Twelve-Factor App]
     *
     * 로컬 개발 환경의 편의성을 위해 '.env' 파일을 지원하지만,
     * 실제 운영(Production) 환경에서는 CI/CD 파이프라인을 통해 주입된 시스템 환경 변수를 우선합니다.
     *
     * 'ignoreIfMissing()' 옵션을 사용하여 배포 환경에서 .env 파일이 없더라도
     * 애플리케이션이 중단되지 않고 시스템 환경 변수로 fallback 되도록 구성했습니다.
     */
    val dotenv = Dotenv.configure().ignoreIfMissing().load()

    // 로드된 환경 변수를 Spring Boot가 인식할 수 있도록 시스템 프로퍼티로 이관(Migration)
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    runApplication<BlogBackendApplication>(*args)
}