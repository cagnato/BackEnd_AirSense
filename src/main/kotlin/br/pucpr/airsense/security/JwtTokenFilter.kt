package br.pucpr.airsense.security

import br.pucpr.airsense.users.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Valida o JWT e confere o usuário no banco a cada requisição: token de usuário apagado
 * deixa de valer na hora, e as roles usadas são as atuais (não as gravadas no token).
 * OncePerRequestFilter evita rodar duas vezes (o Spring Boot também registra @Component como filtro).
 */
@Component
class JwtTokenFilter(
    private val jwt: Jwt,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val tokenUser = jwt.extract(req)?.principal as? UserToken
        val user = tokenUser?.let { userRepository.findByIdOrNull(it.id) }
        if (user != null) {
            val roles = user.roles.map { it.name.uppercase() }.toSet()
            val principal = UserToken(id = user.id!!, login = user.email, name = user.name, roles = roles)
            val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
        }
        chain.doFilter(req, res)
    }
}
