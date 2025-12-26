package me.wypark.blogbackend.domain.auth

import me.wypark.blogbackend.api.dto.LoginRequest
import me.wypark.blogbackend.api.dto.SignupRequest
import me.wypark.blogbackend.api.dto.TokenDto
import me.wypark.blogbackend.core.config.jwt.JwtProvider
import me.wypark.blogbackend.domain.user.Member
import me.wypark.blogbackend.domain.user.MemberRepository
import me.wypark.blogbackend.domain.user.Role
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailService: EmailService
) {

    /**
     * 회원가입
     */
    @Transactional
    fun signup(request: SignupRequest) {
        if (memberRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 가입된 이메일입니다.")
        }
        if (memberRepository.existsByNickname(request.nickname)) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
        }

        val member = Member(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname,
            role = Role.ROLE_USER,
            isVerified = false
        )

        memberRepository.save(member)

        emailService.sendVerificationCode(request.email)
    }

    /**
     * 로그인
     */
    @Transactional
    fun login(request: LoginRequest): TokenDto {
        val member = memberRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("가입되지 않은 이메일입니다.")

        // 비밀번호 체크
        if (!passwordEncoder.matches(request.password, member.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        // 이메일 인증 여부 체크
        if (!member.isVerified) {
            throw IllegalStateException("이메일 인증이 필요합니다.")
        }

        // 1. ID/PW 기반의 인증 토큰 생성
        val authenticationToken = UsernamePasswordAuthenticationToken(request.email, request.password)

        // 2. 실제 검증 (사용자 비밀번호 체크)
        // authenticate() 실행 시 CustomUserDetailsService.loadUserByUsername 실행됨
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        val tokenDto = jwtProvider.generateTokenDto(authentication)

        // 4. RefreshToken Redis 저장 (RTR: 기존 토큰 덮어쓰기)
        refreshTokenRepository.save(authentication.name, tokenDto.refreshToken)

        return tokenDto
    }

    /**
     * 토큰 재발급 (RTR 적용)
     */
    @Transactional
    fun reissue(accessToken: String, refreshToken: String): TokenDto {
        // 1. 리프레시 토큰 검증 (만료 여부, 위변조 여부)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw IllegalArgumentException("유효하지 않은 Refresh Token입니다.")
        }

        // 2. 액세스 토큰에서 User ID(Email) 가져오기 (만료된 토큰이어도 파싱 가능하도록 JwtProvider가 설계됨)
        val authentication = jwtProvider.getAuthentication(accessToken)

        // 3. Redis에서 저장된 Refresh Token 가져오기
        val savedRefreshToken = refreshTokenRepository.findByEmail(authentication.name)
            ?: throw IllegalArgumentException("로그아웃 된 사용자입니다.")

        // 4. 토큰 일치 여부 확인 (재사용 방지)
        if (savedRefreshToken != refreshToken) {
            refreshTokenRepository.delete(authentication.name)
            throw IllegalArgumentException("토큰 정보가 일치하지 않습니다.")
        }

        // 5. 새 토큰 생성 (Rotation)
        val newTokenDto = jwtProvider.generateTokenDto(authentication)

        // 6. Redis 업데이트 (한번 쓴 토큰 폐기 -> 새 토큰 저장)
        refreshTokenRepository.save(authentication.name, newTokenDto.refreshToken)

        return newTokenDto
    }

    /**
     * 로그아웃
     */
    @Transactional
    fun logout(email: String) {
        refreshTokenRepository.delete(email)
    }

    // 3. 이메일 인증 확인
    @Transactional
    fun verifyEmail(email: String, code: String) {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 회원입니다.")

        if (member.isVerified) {
            throw IllegalArgumentException("이미 인증된 회원입니다.")
        }

        // 코드 검증
        if (!emailService.verifyCode(email, code)) {
            throw IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.")
        }

        // 인증 상태 업데이트
        member.verify()
    }
}