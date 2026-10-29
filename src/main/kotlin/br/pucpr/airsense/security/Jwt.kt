package br.pucpr.airsense.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date

@Component
class Jwt(
    @Value("\${airsense.jwt.secret}") private val secret: String
) {

    private val log = LoggerFactory.getLogger(Jwt::class.java)
    private val mapper = jacksonObjectMapper()

    companion object {
        private const val ISSUER = "PUCPR AirSense Server"
        private const val EXPIRE_HOURS = 24L
        private const val PREFIX = "Bearer "
        private const val USER_CLAIM = "user"
    }

    fun createToken(userToken: UserToken): String {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        return Jwts.builder()
            .header().add("typ", "JWT").and()
            .subject(userToken.id.toString())
            .issuer(ISSUER)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(now.plusHours(EXPIRE_HOURS).toInstant()))
            .claim(USER_CLAIM, userToken)
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()), Jwts.SIG.HS256)
            .compact()
    }

    fun extract(req: HttpServletRequest): Authentication? {
        try {
            val header = req.getHeader(AUTHORIZATION)
            if (header == null || !header.startsWith(PREFIX)) return null

            val token = header.removePrefix(PREFIX).trim()
            val claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray()))
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .payload

            val userToken = mapper.convertValue(claims[USER_CLAIM], UserToken::class.java)
            val authorities = userToken.roles.map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") }

            return UsernamePasswordAuthenticationToken.authenticated(userToken, userToken.id, authorities)
        } catch (e: Exception) {
            log.debug("Token JWT rejeitado: {}", e.message)
            return null
        }
    }
}