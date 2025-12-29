package me.wypark.blogbackend

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class BlogBackendApplication

fun main(args: Array<String>) {
    // 1. .env 파일 로드
    val dotenv = Dotenv.configure().ignoreIfMissing().load()

    // 2. 로드한 내용을 시스템 프로퍼티에 설정 (그래야 application.yml에서 ${}로 읽음)
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }

    // 3. 스프링 실행
    runApplication<BlogBackendApplication>(*args)
}
