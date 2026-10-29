package br.pucpr.airsense.exception

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import com.fasterxml.jackson.databind.JsonMappingException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList()
)

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    private fun build(status: HttpStatus, message: String, details: List<String> = emptyList()) =
        ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                details = details
            )
        )

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ErrorResponse> {
        log.warn("Erro de negócio [{}]: {}", ex.status, ex.message)
        return build(ex.status, ex.message ?: "Erro na requisição")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        log.warn("Falha de validação nos campos: {}", errors)
        return build(HttpStatus.BAD_REQUEST, "Os dados enviados são inválidos", errors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("JSON malformado: {}", ex.mostSpecificCause.message)
        // Quando o Jackson sabe qual campo falhou (tipo errado, valor de enum inválido, campo ausente), avisa qual
        val field = (ex.cause as? JsonMappingException)?.path
            ?.mapNotNull { it.fieldName }
            ?.joinToString(".")
            ?.takeIf { it.isNotBlank() }
        val details = if (field != null) listOf("$field: valor ausente ou inválido") else emptyList()
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", details)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaType(ex: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado: ${ex.contentType}. Use application/json")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        val expected = ex.requiredType?.simpleName ?: "desconhecido"
        log.warn("Parâmetro '{}' com tipo inválido: {}", ex.name, ex.value)
        return build(
            HttpStatus.BAD_REQUEST,
            "Valor inválido para o parâmetro '${ex.name}': ${ex.value} (esperado: $expected)"
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: ${ex.parameterName}")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("Violação de integridade: {}", ex.mostSpecificCause.message)
        return build(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        log.warn("Acesso negado: {}", ex.message)
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso")
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        log.warn("Falha de autenticação: {}", ex.message)
        return build(HttpStatus.UNAUTHORIZED, "Autenticação necessária")
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.NOT_FOUND, "Rota não encontrada: /${ex.resourcePath}")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.METHOD_NOT_ALLOWED, "Método ${ex.method} não suportado nesta rota")

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Erro inesperado", ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor")
    }
}
