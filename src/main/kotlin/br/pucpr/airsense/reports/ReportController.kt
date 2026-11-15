package br.pucpr.airsense.reports

import br.pucpr.airsense.security.UserToken
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/reports")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Reports", description = "Gerenciamento e geração de relatórios de qualidade do ar")
class ReportController(private val reportService: ReportService) {

    private fun Authentication.currentUserId(): Long =
        (this.principal as UserToken).id

    @PostMapping
    fun create(
        auth: Authentication,
        @Valid @RequestBody req: ReportRequest
    ): ResponseEntity<ReportResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(reportService.create(auth.currentUserId(), req))

    @GetMapping("/{id}")
    fun findById(
        auth: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.findById(auth.currentUserId(), id))

    @GetMapping
    fun search(
        auth: Authentication,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) environmentId: Long?,
        @RequestParam(required = false) roomId: Long?,
        @RequestParam(required = false) deviceId: Long?,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<List<ReportResponse>> =
        ResponseEntity.ok(
            reportService.search(
                ownerId = auth.currentUserId(),
                startDate = startDate,
                endDate = endDate,
                environmentId = environmentId,
                roomId = roomId,
                deviceId = deviceId,
                sortBy = sortBy,
                sortDir = sortDir
            )
        )

    @PutMapping("/{id}")
    fun update(
        auth: Authentication,
        @PathVariable id: Long,
        @Valid @RequestBody req: ReportRequest
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.update(auth.currentUserId(), id, req))

    @DeleteMapping("/{id}")
    fun delete(
        auth: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        reportService.delete(auth.currentUserId(), id)
        return ResponseEntity.noContent().build()
    }

    // --- Associações: Dispositivo ---

    @PutMapping("/{id}/device/{deviceId}")
    fun associateDevice(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.associateDevice(auth.currentUserId(), id, deviceId))

    @DeleteMapping("/{id}/device/{deviceId}")
    fun disassociateDevice(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable deviceId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.disassociateDevice(auth.currentUserId(), id, deviceId))

    // --- Associações: Cômodo ---

    @PutMapping("/{id}/room/{roomId}")
    fun associateRoom(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable roomId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.associateRoom(auth.currentUserId(), id, roomId))

    @DeleteMapping("/{id}/room/{roomId}")
    fun disassociateRoom(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable roomId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.disassociateRoom(auth.currentUserId(), id, roomId))

    // --- Associações: Ambiente ---

    @PutMapping("/{id}/environment/{environmentId}")
    fun associateEnvironment(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable environmentId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.associateEnvironment(auth.currentUserId(), id, environmentId))

    @DeleteMapping("/{id}/environment/{environmentId}")
    fun disassociateEnvironment(
        auth: Authentication,
        @PathVariable id: Long,
        @PathVariable environmentId: Long
    ): ResponseEntity<ReportResponse> =
        ResponseEntity.ok(reportService.disassociateEnvironment(auth.currentUserId(), id, environmentId))
}