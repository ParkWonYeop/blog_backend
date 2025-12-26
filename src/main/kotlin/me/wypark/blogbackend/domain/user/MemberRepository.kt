package me.wypark.blogbackend.domain.user

import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    // 로그인 및 중복 가입 방지를 위한 핵심 메소드들입니다.
    fun findByEmail(email: String): Member?
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}