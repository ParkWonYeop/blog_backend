package me.wypark.blogbackend.domain.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

/**
 * [Spring Security 사용자 정보 확장 구현체]
 *
 * Spring Security의 표준 UserDetails(User) 클래스를 상속받아
 * 비즈니스 로직에 필요한 추가 식별자들을 포함하도록 확장한 클래스입니다.
 *
 * [설계 의도]
 * 기본 User 객체는 username(email)과 password, 권한 정보만 가지고 있습니다.
 * 하지만 실제 서비스 로직이나 JWT 토큰 생성 시에는 사용자의 DB PK(id)나 닉네임이 자주 필요합니다.
 * 매 요청마다 DB를 다시 조회하는 오버헤드를 줄이기 위해, 인증 객체(Authentication) 내부에
 * 이 정보들을 함께 캐싱(Caching)하여 운반하도록 설계했습니다.
 */
class CustomUserDetails(
    // DB의 Primary Key (비즈니스 로직에서 조인이나 조회 시 사용)
    val memberId: Long,

    // UI 표시용 닉네임 (매번 회원 정보를 조회하지 않기 위함)
    val nickname: String,

    username: String,
    password: String,
    authorities: Collection<GrantedAuthority>
) : User(username, password, authorities)