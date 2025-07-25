package me.wypark.blogbackend.application.auth

import me.wypark.blogbackend.application.common.BusinessException
import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import me.wypark.blogbackend.domain.user.Role
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val tokenProvider: TokenProvider,
    private val refreshTokenStore: RefreshTokenStore,
    private val emailVerification: EmailVerification,
    private val userDetailsService: UserDetailsService
) {

    @Transactional
    fun signup(request: SignupRequest) {
        validateUniqueMember(request)
        memberRepository.save(
            Member(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                nickname = request.nickname,
                role = Role.ROLE_USER,
                isVerified = false
            )
        )
        emailVerification.sendVerificationCode(request.email)
    }
