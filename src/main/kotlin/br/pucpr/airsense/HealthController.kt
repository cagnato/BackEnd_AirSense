package br.pucpr.airsense

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// Rota pública para verificar se o servidor está no ar (não exige token).
@RestController
class HealthController {

    @GetMapping("/health")
    @SecurityRequirements // sem cadeado no Swagger
    fun health(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "UP"))
}
