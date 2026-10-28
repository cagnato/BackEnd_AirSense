package br.pucpr.airsense.security

data class UserToken(
    val id: Long = 0,
    val login: String = "",
    val name: String = "",
    val roles: Set<String> = emptySet()
)