package br.pucpr.airsense.limits

import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull

// Limites físicos aceitos para cada medida (iguais aos do app Android).
object ReadingLimits {
    const val TEMP_MIN = "-10.0"
    const val TEMP_MAX = "50.0"
    const val HUMIDITY_MIN = "0.0"
    const val HUMIDITY_MAX = "100.0"
    const val AIR_QUALITY_MIN = "0.0"
    const val AIR_QUALITY_MAX = "500.0"
    const val CO2_MIN = "300.0"
    const val CO2_MAX = "5000.0"
}

/**
 * Faixas seguras de temperatura, umidade, qualidade do ar e CO2.
 * Ligada por um-para-um (opcional) a um Environment, a um Room ou a um Device.
 */
@Entity
@Table(name = "tbl_safety_limits")
class SafetyLimits(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false) var tempMinSafe: Double = DEFAULT_TEMP_MIN,
    @Column(nullable = false) var tempMaxSafe: Double = DEFAULT_TEMP_MAX,
    @Column(nullable = false) var humidityMinSafe: Double = DEFAULT_HUMIDITY_MIN,
    @Column(nullable = false) var humidityMaxSafe: Double = DEFAULT_HUMIDITY_MAX,
    @Column(nullable = false) var airQualityMinSafe: Double = DEFAULT_AIR_QUALITY_MIN,
    @Column(nullable = false) var airQualityMaxSafe: Double = DEFAULT_AIR_QUALITY_MAX,
    @Column(nullable = false) var co2MinSafe: Double = DEFAULT_CO2_MIN,
    @Column(nullable = false) var co2MaxSafe: Double = DEFAULT_CO2_MAX
) {
    fun apply(req: SafetyLimitsRequest) {
        tempMinSafe = req.tempMinSafe!!
        tempMaxSafe = req.tempMaxSafe!!
        humidityMinSafe = req.humidityMinSafe!!
        humidityMaxSafe = req.humidityMaxSafe!!
        airQualityMinSafe = req.airQualityMinSafe!!
        airQualityMaxSafe = req.airQualityMaxSafe!!
        co2MinSafe = req.co2MinSafe!!
        co2MaxSafe = req.co2MaxSafe!!
    }

    companion object {
        // Padrão do sistema: vale quando nem dispositivo, nem cômodo, nem ambiente têm limites próprios.
        const val DEFAULT_TEMP_MIN = 18.0
        const val DEFAULT_TEMP_MAX = 26.0
        const val DEFAULT_HUMIDITY_MIN = 30.0
        const val DEFAULT_HUMIDITY_MAX = 60.0
        const val DEFAULT_AIR_QUALITY_MIN = 0.0
        const val DEFAULT_AIR_QUALITY_MAX = 100.0
        const val DEFAULT_CO2_MIN = 400.0
        const val DEFAULT_CO2_MAX = 1000.0

        fun from(req: SafetyLimitsRequest): SafetyLimits = SafetyLimits().also { it.apply(req) }
    }
}

/** De onde vem o limite que vale: o mais específico definido. */
enum class LimitsSource { DEVICE, ROOM, ENVIRONMENT, DEFAULT }

// --- DTOs ---

data class SafetyLimitsRequest(
    @field:NotNull(message = "tempMinSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.TEMP_MIN) @field:DecimalMax(ReadingLimits.TEMP_MAX)
    val tempMinSafe: Double?,
    @field:NotNull(message = "tempMaxSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.TEMP_MIN) @field:DecimalMax(ReadingLimits.TEMP_MAX)
    val tempMaxSafe: Double?,

    @field:NotNull(message = "humidityMinSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.HUMIDITY_MIN) @field:DecimalMax(ReadingLimits.HUMIDITY_MAX)
    val humidityMinSafe: Double?,
    @field:NotNull(message = "humidityMaxSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.HUMIDITY_MIN) @field:DecimalMax(ReadingLimits.HUMIDITY_MAX)
    val humidityMaxSafe: Double?,

    @field:NotNull(message = "airQualityMinSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.AIR_QUALITY_MIN) @field:DecimalMax(ReadingLimits.AIR_QUALITY_MAX)
    val airQualityMinSafe: Double?,
    @field:NotNull(message = "airQualityMaxSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.AIR_QUALITY_MIN) @field:DecimalMax(ReadingLimits.AIR_QUALITY_MAX)
    val airQualityMaxSafe: Double?,

    @field:NotNull(message = "co2MinSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.CO2_MIN) @field:DecimalMax(ReadingLimits.CO2_MAX)
    val co2MinSafe: Double?,
    @field:NotNull(message = "co2MaxSafe é obrigatório")
    @field:DecimalMin(ReadingLimits.CO2_MIN) @field:DecimalMax(ReadingLimits.CO2_MAX)
    val co2MaxSafe: Double?
)

data class SafetyLimitsResponse(
    val tempMinSafe: Double,
    val tempMaxSafe: Double,
    val humidityMinSafe: Double,
    val humidityMaxSafe: Double,
    val airQualityMinSafe: Double,
    val airQualityMaxSafe: Double,
    val co2MinSafe: Double,
    val co2MaxSafe: Double
) {
    constructor(limits: SafetyLimits) : this(
        tempMinSafe = limits.tempMinSafe,
        tempMaxSafe = limits.tempMaxSafe,
        humidityMinSafe = limits.humidityMinSafe,
        humidityMaxSafe = limits.humidityMaxSafe,
        airQualityMinSafe = limits.airQualityMinSafe,
        airQualityMaxSafe = limits.airQualityMaxSafe,
        co2MinSafe = limits.co2MinSafe,
        co2MaxSafe = limits.co2MaxSafe
    )

    companion object {
        val DEFAULT = SafetyLimitsResponse(SafetyLimits())
    }
}

/** Resposta dos endpoints /limits: o limite próprio (ou nulo) e o que vale de fato, com a origem. */
data class LimitsView(
    val limits: SafetyLimitsResponse?,
    val effectiveLimits: SafetyLimitsResponse,
    val limitsSource: LimitsSource
)
