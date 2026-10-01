package me.wypark.blogbackend.global.config

import com.fasterxml.jackson.core.StreamReadConstraints
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun streamReadConstraintsCustomizer() = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.postConfigurer { objectMapper ->
            objectMapper.factory.setStreamReadConstraints(
                StreamReadConstraints.builder()
                    .maxNestingDepth(100)
                    .maxNumberLength(1_000)
                    .maxStringLength(1_100_000)
                    .build()
            )
        }
    }
}
