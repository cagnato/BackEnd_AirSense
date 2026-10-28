package br.pucpr.airsense.environments

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/environments")
class EnvironmentController(private val environmentService: EnvironmentService) {

    @PostMapping
    fun create(@Valid @RequestBody req: EnvironmentRequest): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(environmentService.create(req))

    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(defaultValue = "name") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String
    ): ResponseEntity<List<EnvironmentResponse>> =
        ResponseEntity.ok(environmentService.search(name, city, sortBy, sortDir))

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.ok(environmentService.findById(id))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: EnvironmentRequest
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.ok(environmentService.update(id, req))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        environmentService.delete(id)
        return ResponseEntity.noContent().build()
    }

    // Endpoints de associação: Adicionar e Remover Cômodos (Micro) de um Ambiente (Macro)
    @PostMapping("/{id}/rooms")
    fun addRoom(
        @PathVariable id: Long,
        @Valid @RequestBody req: RoomRequest
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(environmentService.addRoom(id, req))

    @DeleteMapping("/{id}/rooms/{roomId}")
    fun removeRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long
    ): ResponseEntity<EnvironmentResponse> =
        ResponseEntity.ok(environmentService.removeRoom(id, roomId))
}