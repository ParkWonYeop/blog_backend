package me.wypark.blogbackend.core.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing // 엔티티의 생성일/수정일 자동 주입 활성화
class JpaConfig