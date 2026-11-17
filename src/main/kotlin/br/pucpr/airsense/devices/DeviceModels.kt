package br.pucpr.airsense.devices

import br.pucpr.airsense.environments.Environment
import br.pucpr.airsense.environments.Room
import br.pucpr.airsense.users.User
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import br.pucpr.airsense.limits.LimitsSource
import br.pucpr.airsense.limits.LimitsView
import br.pucpr.airsense.limits.SafetyLimits
import br.pucpr.airsense.limits.SafetyLimitsRequest
import br.pucpr.airsense.limits.SafetyLimitsResponse
import br.pucpr.airsense.limits.SafetyLimitsRules
import jakarta.validation.Valid
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

    // Limites próprios (opcionais). Sem eles, valem os do cômodo, do ambiente ou o padrão.
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "limits_id", unique = true)
    var limits: SafetyLimits? = null,

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

    // Opcional. Nulo = sem limites próprios (herda do cômodo, do ambiente ou o padrão).
    @field:Valid
    val limits: SafetyLimitsRequest? = null
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
    val limits: SafetyLimitsResponse?,
    val effectiveLimits: SafetyLimitsResponse,
    val limitsSource: LimitsSource
) {
    private constructor(device: Device, view: LimitsView) : this(
        id = device.id!!,
        deviceId = device.deviceId,
        name = device.name,
        status = device.status,
        statusCode = device.status.code,
        environmentId = device.environment?.id,
        environmentName = device.environment?.name,
        roomId = device.room?.id,
        roomName = device.room?.name,
        limits = view.limits,
        effectiveLimits = view.effectiveLimits,
        limitsSource = view.limitsSource
    )

    constructor(device: Device) : this(device, device.limitsView())
}

/** Limites próprios do dispositivo e os que valem (dispositivo -> cômodo -> ambiente -> padrão). */
fun Device.limitsView(): LimitsView =
    SafetyLimitsRules.view(own = limits, device = limits, room = room?.limits, environment = environment?.limits)
