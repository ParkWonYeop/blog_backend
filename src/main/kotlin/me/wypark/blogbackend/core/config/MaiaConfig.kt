package me.wypark.blogbackend.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(MaiaProperties::class)
class MaiaConfig {

    @Bean
    fun maiaRestClient(
        builder: RestClient.Builder,
        properties: MaiaProperties
    ): RestClient {
        return builder
            .baseUrl(properties.engineUrl)
            .build()
    }
}

@ConfigurationProperties(prefix = "maia")
data class MaiaProperties(
    val engineUrl: String = "http://localhost:8000",
    val gameSessionTtl: Duration = Duration.ofHours(6)
)
