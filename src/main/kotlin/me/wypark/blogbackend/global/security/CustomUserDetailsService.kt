package me.wypark.blogbackend.global.security

import me.wypark.blogbackend.global.security.AuthenticatedUser
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.repository.MemberRepository
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
        return AuthenticatedUser(
            memberId = requireNotNull(member.id) { "Persisted member must have an id" },
            nickname = member.nickname,
            username = member.email,
            password = member.password,
            authorities = listOf(SimpleGrantedAuthority(member.role.name))
        )
    }
}
