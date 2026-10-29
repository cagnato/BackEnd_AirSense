package br.pucpr.airsense.environments

import br.pucpr.airsense.security.UserToken
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/environments")
class EnvironmentController(private val environmentService: EnvironmentService) {

    @PostMapping
    fun create(
        @Valid @RequestBody req: EnvironmentRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(environmentService.create(user.id, req))

    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(defaultValue = "name") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<List<EnvironmentResponse>> =
        ResponseEntity.ok(environmentService.search(user.id, name, city, sortBy, sortDir))

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.ok(environmentService.findById(user.id, id))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: EnvironmentRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.ok(environmentService.update(user.id, id, req))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        environmentService.delete(user.id, id)
        return ResponseEntity.noContent().build()
    }

    // --- Cômodos (Micro) de um Ambiente (Macro) ---

    @PostMapping("/{id}/rooms")
    fun addRoom(
        @PathVariable id: Long,
        @Valid @RequestBody req: RoomRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<RoomResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(environmentService.addRoom(user.id, id, req))

    @GetMapping("/{id}/rooms")
    fun listRooms(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<List<RoomResponse>> =
        ResponseEntity.ok(environmentService.listRooms(user.id, id))

    @PutMapping("/{id}/rooms/{roomId}")
    fun updateRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @Valid @RequestBody req: RoomRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<RoomResponse> =
        ResponseEntity.ok(environmentService.updateRoom(user.id, id, roomId, req))

    @DeleteMapping("/{id}/rooms/{roomId}")
    fun removeRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        environmentService.removeRoom(user.id, id, roomId)
        return ResponseEntity.noContent().build()
    }
}
