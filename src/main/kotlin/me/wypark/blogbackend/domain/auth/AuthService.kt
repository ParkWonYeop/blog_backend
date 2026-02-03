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
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [인증 비즈니스 로직 서비스]
 *
 * 회원가입, 로그인, 토큰 재발급 등 계정 보안과 관련된 핵심 로직을 담당합니다.
 * DB(Member), Redis(RefreshToken), Email(Verification) 등 여러 인프라 자원을 오케스트레이션하여
 * 안전하고 무결한 인증 프로세스를 보장합니다.
 */
@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailService: EmailService,
    private val userDetailsService: UserDetailsService
) {

    /**
     * 신규 회원을 등록합니다.
     *
     * [스팸 방지 전략]
     * 무분별한 가입을 막기 위해 가입 즉시 활성화(Active)하지 않고,
     * `isVerified = false` 상태로 저장한 뒤 이메일 인증을 강제합니다.
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
            isVerified = false // 초기 상태는 미인증
        )

        memberRepository.save(member)

        // 비동기 처리를 고려할 수 있으나, 가입 직후 메일 수신이 중요하므로 동기 호출
        emailService.sendVerificationCode(request.email)
    }

    /**
     * 사용자 자격 증명을 검증하고 초기 토큰을 발급합니다.
     *
     * 단순 ID/PW 검사뿐만 아니라, 이메일 인증 여부(Business Rule)를 체크하여
     * 미인증 계정의 접근을 원천 차단합니다.
     */
    @Transactional
    fun login(request: LoginRequest): TokenDto {
        val member = memberRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("가입되지 않은 이메일입니다.")

        // 비밀번호 체크 (Bcrypt)
        if (!passwordEncoder.matches(request.password, member.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        // 계정 활성화 여부 체크
        if (!member.isVerified) {
            throw IllegalStateException("이메일 인증이 필요합니다.")
        }

        // 1. Spring Security 인증 토큰 생성
        val authenticationToken = UsernamePasswordAuthenticationToken(request.email, request.password)

        // 2. 실제 검증 수행 (CustomUserDetailsService 호출됨)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)

        // 3. 인증 정보를 기반으로 JWT(Access + Refresh) 생성
        val tokenDto = jwtProvider.generateTokenDto(authentication)

        // 4. Refresh Token을 Redis에 저장 (RTR 전략의 기준점)
        refreshTokenRepository.save(authentication.name, tokenDto.refreshToken)

        return tokenDto
    }

    /**
     * Access Token 만료 시 토큰을 갱신합니다.
     *
     * [핵심 보안 전략: Refresh Token Rotation (RTR)]
     * 보안성을 높이기 위해 Refresh Token을 일회용으로 사용합니다.
     * 토큰 재발급 요청 시 기존 Refresh Token을 폐기하고, 새로운 Refresh Token을 발급합니다.
     *
     * [토큰 탈취 감지]
     * 만약 이미 사용된(폐기된) Refresh Token으로 요청이 들어온다면, 이는 토큰이 탈취된 것으로 간주하여
     * 해당 사용자의 저장된 모든 토큰을 삭제하고 강제 로그아웃 처리합니다.
     */
    @Transactional
    fun reissue(accessToken: String, refreshToken: String): TokenDto {
        // 1. 토큰 자체의 유효성 검증 (위변조 여부)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw IllegalArgumentException("유효하지 않은 Refresh Token입니다.")
        }

        // 2. Access Token에서 사용자 정보 추출
        val tempAuthentication = jwtProvider.getAuthentication(accessToken)

        // 3. Redis에 저장된 최신 Refresh Token 조회
        val savedRefreshToken = refreshTokenRepository.findByEmail(tempAuthentication.name)
            ?: throw IllegalArgumentException("로그아웃 된 사용자입니다.")

        // 4. [RTR 핵심] 토큰 불일치 감지 (재사용 시도 -> 탈취 의심)
        if (savedRefreshToken != refreshToken) {
            refreshTokenRepository.delete(tempAuthentication.name) // 보안 조치: 세션 전체 파기
            throw IllegalArgumentException("토큰 정보가 일치하지 않습니다. (재사용 감지됨)")
        }

        // 5. DB에서 최신 유저 정보 다시 로드
        // (토큰 갱신 시점의 권한 변경이나 닉네임 변경 등을 반영하기 위함)
        val userDetails = userDetailsService.loadUserByUsername(tempAuthentication.name)

        // 6. 새로운 Authentication 객체 생성
        val newAuthentication = UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )

        // 7. 새 토큰 쌍 발급 (Rotate)
        val newTokenDto = jwtProvider.generateTokenDto(newAuthentication)

        // 8. Redis 업데이트 (기존 토큰 덮어쓰기)
        refreshTokenRepository.save(newAuthentication.name, newTokenDto.refreshToken)

        return newTokenDto
    }

    /**
     * 로그아웃 처리
     *
     * 서버 측에서 Refresh Token을 삭제함으로써, Access Token이 만료되는 즉시
     * 더 이상 갱신할 수 없도록 세션을 종료시킵니다.
     */
    @Transactional
    fun logout(email: String) {
        refreshTokenRepository.delete(email)
    }

    /**
     * 이메일 인증 코드를 검증하고 계정 상태를 활성화(Verify)합니다.
     * 상태 변경(update)이 발생하므로 트랜잭션 내에서 처리됩니다.
     */
    @Transactional
    fun verifyEmail(email: String, code: String) {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 회원입니다.")

        if (member.isVerified) {
            throw IllegalArgumentException("이미 인증된 회원입니다.")
        }

        // Redis에 저장된 코드와 대조
        if (!emailService.verifyCode(email, code)) {
            throw IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.")
        }

        // 인증 성공 시 회원 상태 변경
        member.verify()
    }
}