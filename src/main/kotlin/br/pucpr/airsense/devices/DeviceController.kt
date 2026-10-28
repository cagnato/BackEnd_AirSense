package br.pucpr.airsense.devices

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/devices")
class DeviceController(private val deviceService: DeviceService) {

    @PostMapping
    fun create(@Valid @RequestBody req: DeviceRequest): ResponseEntity<DeviceResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(deviceService.create(req))

    @GetMapping
    fun findAll(): ResponseEntity<List<DeviceResponse>> =
        ResponseEntity.ok(deviceService.findAll())

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.findById(id))

    @PutMapping("/{id}/bind/{roomId}")
    fun bindToRoom(
        @PathVariable id: Long,
        @PathVariable roomId: Long
    ): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.bindToRoom(id, roomId))

    @DeleteMapping("/{id}/unbind")
    fun unbindFromRoom(@PathVariable id: Long): ResponseEntity<DeviceResponse> =
        ResponseEntity.ok(deviceService.unbindFromRoom(id))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        deviceService.delete(id)
        return ResponseEntity.noContent().build()
    }
}