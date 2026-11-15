package br.pucpr.airsense.reports

import br.pucpr.airsense.devices.Device
import br.pucpr.airsense.environments.Environment
import br.pucpr.airsense.environments.Room
import br.pucpr.airsense.users.User
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "tbl_reports")
class Report(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
    var device: Device? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    var room: Room? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = true)
    var environment: Environment? = null,

    @Column(nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var endDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    // --- D33: Nomes guardados para preservar histórico caso o vínculo seja excluído ---
    var deviceName: String? = null,
    var roomName: String? = null,
    var environmentName: String? = null,

    // --- D32: Resumo guardado das leituras no período ---
    @Column(nullable = false)
    var readingsCount: Int = 0,

    @Column(nullable = false)
    var outOfRangeCount: Int = 0,

    var minAqi: Double? = null,
    var avgAqi: Double? = null,
    var maxAqi: Double? = null,

    var minTemperature: Double? = null,
    var avgTemperature: Double? = null,
    var maxTemperature: Double? = null,

    var minHumidity: Double? = null,
    var avgHumidity: Double? = null,
    var maxHumidity: Double? = null,

    var minCo2: Double? = null,
    var avgCo2: Double? = null,
    var maxCo2: Double? = null,

    @Column(nullable = false)
    var generatedAt: Instant = Instant.now()
)

// --- DTOs ---

data class ReportRequest(
    @field:NotNull(message = "A data inicial (startDate) é obrigatória")
    val startDate: LocalDate?,

    @field:NotNull(message = "A data final (endDate) é obrigatória")
    val endDate: LocalDate?,

    val environmentId: Long? = null,
    val roomId: Long? = null,
    val deviceId: Long? = null
)

data class ReportResponse(
    val id: Long,
    val ownerId: Long,
    val deviceId: Long?,
    val deviceName: String?,
    val roomId: Long?,
    val roomName: String?,
    val environmentId: Long?,
    val environmentName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Instant,
    val generatedAt: Instant,
    val readingsCount: Int,
    val outOfRangeCount: Int,
    val minAqi: Double?,
    val avgAqi: Double?,
    val maxAqi: Double?,
    val minTemperature: Double?,
    val avgTemperature: Double?,
    val maxTemperature: Double?,
    val minHumidity: Double?,
    val avgHumidity: Double?,
    val maxHumidity: Double?,
    val minCo2: Double?,
    val avgCo2: Double?,
    val maxCo2: Double?
) {
    constructor(report: Report) : this(
        id = report.id!!,
        ownerId = report.owner.id!!,
        deviceId = report.device?.id,
        deviceName = report.device?.name ?: report.deviceName,
        roomId = report.room?.id,
        roomName = report.room?.name ?: report.roomName,
        environmentId = report.environment?.id,
        environmentName = report.environment?.name ?: report.environmentName,
        startDate = report.startDate,
        endDate = report.endDate,
        createdAt = report.createdAt,
        generatedAt = report.generatedAt,
        readingsCount = report.readingsCount,
        outOfRangeCount = report.outOfRangeCount,
        minAqi = report.minAqi,
        avgAqi = report.avgAqi,
        maxAqi = report.maxAqi,
        minTemperature = report.minTemperature,
        avgTemperature = report.avgTemperature,
        maxTemperature = report.maxTemperature,
        minHumidity = report.minHumidity,
        avgHumidity = report.avgHumidity,
        maxHumidity = report.maxHumidity,
        minCo2 = report.minCo2,
        avgCo2 = report.avgCo2,
        maxCo2 = report.maxCo2
    )
}