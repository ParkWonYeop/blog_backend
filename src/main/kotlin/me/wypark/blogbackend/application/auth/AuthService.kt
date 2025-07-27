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

    @Transactional
    fun login(request: LoginRequest): TokenDto {
        val member = memberRepository.findByEmail(request.email)
            ?: throw BusinessException("가입되지 않은 이메일입니다.")
        if (!passwordEncoder.matches(request.password, member.password)) {
            throw BusinessException("비밀번호가 일치하지 않습니다.")
        }
        if (!member.isVerified) {
            throw BusinessException("이메일 인증이 필요합니다.")
        }

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val tokens = tokenProvider.generate(authentication)
        refreshTokenStore.save(authentication.name, tokens.refreshToken)
        return tokens
    }

    @Transactional
    fun reissue(accessToken: String, refreshToken: String): TokenDto {
        if (!tokenProvider.isValid(refreshToken)) {
            throw BusinessException("유효하지 않은 Refresh Token입니다.")
        }

        val email = tokenProvider.extractSubject(accessToken)
        val savedToken = refreshTokenStore.findByEmail(email)
            ?: throw BusinessException("로그아웃 된 사용자입니다.")
        if (savedToken != refreshToken) {
            refreshTokenStore.delete(email)
            throw BusinessException("토큰 정보가 일치하지 않습니다. (재사용 감지됨)")
        }

