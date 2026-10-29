package br.pucpr.airsense.devices

import br.pucpr.airsense.environments.Environment
import br.pucpr.airsense.environments.Room
import br.pucpr.airsense.users.User
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


//Mesmo enum do app Android (br.airsense.model.DeviceStatus)
enum class DeviceStatus(val code: Int) {
    OFF(0), ACTIVE(1), NOT_WORKING(2);

    @JsonValue
    fun toJson(): String = name

    companion object {
        // Aceita o nome ("ACTIVE") ou o valor numérico (1 / "1").
        @JvmStatic
        @JsonCreator
        fun from(value: Any?): DeviceStatus {
            val text = value?.toString()?.trim() ?: throw IllegalArgumentException("status nulo")
            text.toIntOrNull()?.let { code ->
                return entries.firstOrNull { it.code == code }
                    ?: throw IllegalArgumentException("Código de status inválido: $code")
            }
            return entries.firstOrNull { it.name.equals(text, ignoreCase = true) }
                ?: throw IllegalArgumentException("Status inválido: $text")
        }
    }
}

@Converter(autoApply = true)
class DeviceStatusConverter : AttributeConverter<DeviceStatus, Int> {
    override fun convertToDatabaseColumn(attribute: DeviceStatus?): Int? = attribute?.code
    override fun convertToEntityAttribute(dbData: Int?): DeviceStatus? =
        dbData?.let { code -> DeviceStatus.entries.first { it.code == code } }
}

// Limites absolutos das faixas
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

// Entidade atualizada para refletir o app
@Entity
@Table(name = "tbl_devices")
class Device(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    // Serial/MAC do dispositivo, ex: DEV-1A2B3C4D
    @Column(unique = true, nullable = false)
    var deviceId: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var status: DeviceStatus = DeviceStatus.ACTIVE,

    @Column(nullable = false) var tempMinSafe: Double = 18.0,
    @Column(nullable = false) var tempMaxSafe: Double = 26.0,
    @Column(nullable = false) var humidityMinSafe: Double = 30.0,
    @Column(nullable = false) var humidityMaxSafe: Double = 60.0,
    @Column(nullable = false) var airQualityMinSafe: Double = 0.0,
    @Column(nullable = false) var airQualityMaxSafe: Double = 100.0,
    @Column(nullable = false) var co2MinSafe: Double = 400.0,
    @Column(nullable = false) var co2MaxSafe: Double = 1000.0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id")
    var environment: Environment? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    var room: Room? = null
)

// --- DTOs ---

data class DeviceRequest(
    // Opcional: se vier vazio, o servidor gera um no formato DEV-XXXXXXXX
    @field:Size(max = 64, message = "O deviceId deve ter no máximo 64 caracteres")
    val deviceId: String? = null,

    @field:NotBlank(message = "O nome do dispositivo é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    val status: DeviceStatus = DeviceStatus.ACTIVE,

    // Opcional. Sem roomId, o device fica só no ambiente (ou em nenhum).
    val environmentId: Long? = null,

    // Opcional. Se informado, o ambiente do device passa a ser o do cômodo.
    val roomId: Long? = null,

    @field:DecimalMin(ReadingLimits.TEMP_MIN) @field:DecimalMax(ReadingLimits.TEMP_MAX)
    val tempMinSafe: Double = 18.0,
    @field:DecimalMin(ReadingLimits.TEMP_MIN) @field:DecimalMax(ReadingLimits.TEMP_MAX)
    val tempMaxSafe: Double = 26.0,

    @field:DecimalMin(ReadingLimits.HUMIDITY_MIN) @field:DecimalMax(ReadingLimits.HUMIDITY_MAX)
    val humidityMinSafe: Double = 30.0,
    @field:DecimalMin(ReadingLimits.HUMIDITY_MIN) @field:DecimalMax(ReadingLimits.HUMIDITY_MAX)
    val humidityMaxSafe: Double = 60.0,

    @field:DecimalMin(ReadingLimits.AIR_QUALITY_MIN) @field:DecimalMax(ReadingLimits.AIR_QUALITY_MAX)
    val airQualityMinSafe: Double = 0.0,
    @field:DecimalMin(ReadingLimits.AIR_QUALITY_MIN) @field:DecimalMax(ReadingLimits.AIR_QUALITY_MAX)
    val airQualityMaxSafe: Double = 100.0,

    @field:DecimalMin(ReadingLimits.CO2_MIN) @field:DecimalMax(ReadingLimits.CO2_MAX)
    val co2MinSafe: Double = 400.0,
    @field:DecimalMin(ReadingLimits.CO2_MIN) @field:DecimalMax(ReadingLimits.CO2_MAX)
    val co2MaxSafe: Double = 1000.0
)

data class DeviceResponse(
    val id: Long,
    val deviceId: String,
    val name: String,
    val status: DeviceStatus,
    val statusCode: Int,
    val environmentId: Long?,
    val environmentName: String?,
    val roomId: Long?,
    val roomName: String?,
    val tempMinSafe: Double,
    val tempMaxSafe: Double,
    val humidityMinSafe: Double,
    val humidityMaxSafe: Double,
    val airQualityMinSafe: Double,
    val airQualityMaxSafe: Double,
    val co2MinSafe: Double,
    val co2MaxSafe: Double
) {
    constructor(device: Device) : this(
        id = device.id!!,
        deviceId = device.deviceId,
        name = device.name,
        status = device.status,
        statusCode = device.status.code,
        environmentId = device.environment?.id,
        environmentName = device.environment?.name,
        roomId = device.room?.id,
        roomName = device.room?.name,
        tempMinSafe = device.tempMinSafe,
        tempMaxSafe = device.tempMaxSafe,
        humidityMinSafe = device.humidityMinSafe,
        humidityMaxSafe = device.humidityMaxSafe,
        airQualityMinSafe = device.airQualityMinSafe,
        airQualityMaxSafe = device.airQualityMaxSafe,
        co2MinSafe = device.co2MinSafe,
        co2MaxSafe = device.co2MaxSafe
    )
}
