package br.pucpr.airsense.devices

import br.pucpr.airsense.security.UserToken
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/devices")
class DeviceController(private val deviceService: DeviceService) {

    @PostMapping
    fun create(
        @Valid @RequestBody req: DeviceRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(deviceService.create(user.id, req))

    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) status: DeviceStatus?,
        @RequestParam(required = false) roomId: Long?,
        @RequestParam(required = false) environmentId: Long?,
        @RequestParam(defaultValue = "id") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<List<DeviceResponse>> =
        ResponseEntity.ok(deviceService.search(user.id, name, status, roomId, environmentId, sortBy, sortDir))

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.findById(user.id, id))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: DeviceRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.update(user.id, id, req))

    @PutMapping("/{id}/bind/{roomId}")
    fun bindToRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.bindToRoom(user.id, id, roomId))

    @DeleteMapping("/{id}/unbind")
    fun unbindFromRoom(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.unbindFromRoom(user.id, id))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        deviceService.delete(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
