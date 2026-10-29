package br.pucpr.airsense.security

import br.pucpr.airsense.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

// Respostas JSON para 401 (sem token / token inválido) e 403 (sem permissão) do filtro de segurança.
@Component
class RestSecurityHandlers(private val mapper: ObjectMapper) : AuthenticationEntryPoint, AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) = write(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária: envie um token JWT válido")

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) = write(response, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso")

    private fun write(response: HttpServletResponse, status: HttpStatus, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        mapper.writeValue(
            response.outputStream,
            ErrorResponse(status = status.value(), error = status.reasonPhrase, message = message)
        )
    }
}
