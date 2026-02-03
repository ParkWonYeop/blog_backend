package me.wypark.blogbackend.domain.auth

import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * [Spring Security 사용자 로드 서비스]
 *
 * Spring Security의 인증 매니저(AuthenticationManager)가 실제 DB에 저장된 사용자 정보를
 * 조회할 수 있도록 지원하는 핵심 인터페이스(UserDetailsService)의 구현체입니다.
 *
 * 도메인 영역의 [Member] 엔티티를 시큐리티 영역의 [UserDetails] 객체로 변환(Adapt)하는 역할을 수행합니다.
 */
@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {

    /**
     * 사용자의 식별자(여기서는 이메일)로 DB에서 사용자 정보를 조회합니다.
     * 로그인 요청 시 내부적으로 호출되며, 조회 실패 시 시큐리티 규격에 맞는 예외를 던집니다.
     */
    override fun loadUserByUsername(username: String): UserDetails {
        return memberRepository.findByEmail(username)
            ?.let { createUserDetails(it) }
            ?: throw UsernameNotFoundException("해당 유저를 찾을 수 없습니다: $username")
    }

    /**
     * [UserDetails 변환 로직]
     *
     * 조회된 Member 엔티티를 기반으로 인증 객체(CustomUserDetails)를 생성합니다.
     *
     * [최적화 전략]
     * Spring Security가 제공하는 기본 User 객체 대신, 직접 정의한 CustomUserDetails를 반환함으로써
     * 추후 컨트롤러나 서비스 계층에서 @AuthenticationPrincipal을 통해
     * DB 추가 조회 없이도 사용자 식별자(ID)와 닉네임에 즉시 접근할 수 있도록 설계했습니다.
     */
    private fun createUserDetails(member: Member): UserDetails {
        return CustomUserDetails(
            memberId = member.id!!,      // 비즈니스 로직용 PK 캐싱
            nickname = member.nickname,  // UI 렌더링용 닉네임 캐싱
            username = member.email,
            password = member.password,
            authorities = listOf(SimpleGrantedAuthority(member.role.name))
        )
    }
}