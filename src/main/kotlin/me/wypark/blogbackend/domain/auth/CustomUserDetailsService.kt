package me.wypark.blogbackend.domain.auth

import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        return memberRepository.findByEmail(username)
            ?.let { createUserDetails(it) }
            ?: throw UsernameNotFoundException("해당 유저를 찾을 수 없습니다: $username")
    }

    private fun createUserDetails(member: Member): UserDetails {
        // [수정] 표준 User 객체 대신, ID와 닉네임을 포함하는 CustomUserDetails 반환
        return CustomUserDetails(
            memberId = member.id!!,      // 토큰에 넣을 ID
            nickname = member.nickname,  // 토큰에 넣을 닉네임
            username = member.email,
            password = member.password,
            authorities = listOf(SimpleGrantedAuthority(member.role.name))
        )
    }
}