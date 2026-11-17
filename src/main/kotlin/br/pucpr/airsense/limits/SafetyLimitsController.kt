package br.pucpr.airsense.limits

import br.pucpr.airsense.security.UserToken
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Limites seguros", description = "Faixas seguras por ambiente, cômodo e dispositivo, com herança")
class SafetyLimitsController(private val service: SafetyLimitsService) {

    // --- Ambiente ---

    @GetMapping("/environments/{id}/limits")
    fun getEnvironment(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.getForEnvironment(user.id, id))

    @PutMapping("/environments/{id}/limits")
    fun setEnvironment(
        @PathVariable id: Long,
        @Valid @RequestBody req: SafetyLimitsRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.setForEnvironment(user.id, id, req))

    @DeleteMapping("/environments/{id}/limits")
    fun removeEnvironment(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        service.removeFromEnvironment(user.id, id)
        return ResponseEntity.noContent().build()
    }

    // --- Cômodo ---

    @GetMapping("/environments/{id}/rooms/{roomId}/limits")
    fun getRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.getForRoom(user.id, id, roomId))

    @PutMapping("/environments/{id}/rooms/{roomId}/limits")
    fun setRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody req: SafetyLimitsRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.setForRoom(user.id, id, roomId, req))

    @DeleteMapping("/environments/{id}/rooms/{roomId}/limits")
    fun removeRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        service.removeFromRoom(user.id, id, roomId)
        return ResponseEntity.noContent().build()
    }

    // --- Dispositivo ---

    @GetMapping("/devices/{id}/limits")
    fun getDevice(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.getForDevice(user.id, id))

    @PutMapping("/devices/{id}/limits")
    fun setDevice(
        @PathVariable id: Long,
        @Valid @RequestBody req: SafetyLimitsRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<LimitsView> = ResponseEntity.ok(service.setForDevice(user.id, id, req))

    @DeleteMapping("/devices/{id}/limits")
    fun removeDevice(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        service.removeFromDevice(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
