package me.wypark.blogbackend.application.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class AuthenticatedUser(
    val memberId: Long,

    val nickname: String,

    username: String,
    password: String,
    authorities: Collection<GrantedAuthority>
) : User(username, password, authorities)
