package me.wypark.blogbackend.domain.auth.service
import me.wypark.blogbackend.domain.auth.dto.LoginRequest
import me.wypark.blogbackend.domain.auth.dto.SignupRequest
import me.wypark.blogbackend.domain.auth.dto.TokenDto

import me.wypark.blogbackend.global.common.BusinessException
import me.wypark.blogbackend.domain.user.entity.Member
import me.wypark.blogbackend.domain.user.repository.MemberRepository
import me.wypark.blogbackend.domain.user.entity.Role
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.AuthenticationException
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
        validatePasswordLength(request.password)
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
        validatePasswordLength(request.password)
        val authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
        } catch (_: AuthenticationException) {
            throw BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        val member = memberRepository.findByEmail(request.email)
            ?: throw BusinessException("이메일 또는 비밀번호가 올바르지 않습니다.")
        if (!member.isVerified) {
            throw BusinessException("이메일 인증이 필요합니다.")
        }

        val tokens = tokenProvider.generate(authentication)
        refreshTokenStore.save(authentication.name, tokens.refreshToken)
        return tokens
    }

    @Transactional
    fun reissue(refreshToken: String): TokenDto {
        if (!tokenProvider.isValidRefreshToken(refreshToken)) {
            throw BusinessException("유효하지 않은 Refresh Token입니다.")
        }

        val email = tokenProvider.extractSubject(refreshToken)
        if (!refreshTokenStore.matches(email, refreshToken)) {
            refreshTokenStore.delete(email)
            throw BusinessException("토큰 정보가 일치하지 않습니다. (재사용 감지됨)")
        }

        val userDetails = userDetailsService.loadUserByUsername(email)
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )
        val tokens = tokenProvider.generate(authentication)
        refreshTokenStore.save(authentication.name, tokens.refreshToken)
        return tokens
    }

    @Transactional
    fun logout(email: String) {
        refreshTokenStore.delete(email)
    }

    @Transactional
    fun verifyEmail(email: String, code: String) {
        val member = memberRepository.findByEmail(email)
            ?: throw BusinessException("존재하지 않는 회원입니다.")
        if (member.isVerified) {
            throw BusinessException("이미 인증된 회원입니다.")
        }
        if (!emailVerification.verifyCode(email, code)) {
            throw BusinessException("인증 코드가 올바르지 않거나 만료되었습니다.")
        }
        member.verify()
    }

    private fun validateUniqueMember(request: SignupRequest) {
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException("이미 가입된 이메일입니다.")
        }
        if (memberRepository.existsByNickname(request.nickname)) {
            throw BusinessException("이미 사용 중인 닉네임입니다.")
        }
    }

    private fun validatePasswordLength(password: String) {
        if (password.toByteArray(Charsets.UTF_8).size > BCRYPT_MAX_BYTES) {
            throw BusinessException("비밀번호는 UTF-8 기준 72바이트 이하로 입력해주세요.")
        }
    }

    companion object {
        private const val BCRYPT_MAX_BYTES = 72
    }
}
