package br.pucpr.airsense.environments

import br.pucpr.airsense.limits.LimitsSource
import br.pucpr.airsense.limits.LimitsView
import br.pucpr.airsense.limits.SafetyLimits
import br.pucpr.airsense.limits.SafetyLimitsRequest
import br.pucpr.airsense.limits.SafetyLimitsResponse
import br.pucpr.airsense.limits.SafetyLimitsRules
import br.pucpr.airsense.users.User
import jakarta.validation.Valid
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Entity
@Table(name = "tbl_environments")
class Environment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = true)
    var description: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User? = null,

    // Limites próprios (opcionais). Sem eles, vale o nível acima ou o padrão do sistema.
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "limits_id", unique = true)
    var limits: SafetyLimits? = null,

    @OneToMany(mappedBy = "environment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val rooms: MutableList<Room> = mutableListOf()
)

// O dono do Room é o dono do Environment ao qual ele pertence.
@Entity
@Table(name = "tbl_rooms")
class Room(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    @ColumnDefault("''")
    var description: String = "",

    @Column(nullable = false)
    var areaM2: Double = 0.0,

    // Limites próprios (opcionais). Sem eles, vale o nível acima ou o padrão do sistema.
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "limits_id", unique = true)
    var limits: SafetyLimits? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    var environment: Environment? = null
)

// --- DTOs ---

data class EnvironmentRequest(
    @field:NotBlank(message = "O nome do ambiente é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    // Opcional: sem descrição, fica vazia (o app não exige)
    @field:Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    val description: String = "",

    // Opcional. Nulo = sem limites próprios.
    @field:Valid
    val limits: SafetyLimitsRequest? = null
)

data class RoomRequest(
    @field:NotBlank(message = "O nome do cômodo é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    // Opcional: sem descrição, fica vazia
    @field:Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    val description: String = "",

    @field:Positive(message = "A área em m² deve ser maior que zero")
    @field:DecimalMax(value = "100000.0", message = "A área deve ser de no máximo 100000 m²")
    val areaM2: Double,

    // Opcional. Nulo = sem limites próprios.
    @field:Valid
    val limits: SafetyLimitsRequest? = null
)

data class RoomResponse(
    val id: Long,
    val name: String,
    val description: String,
    val areaM2: Double,
    val environmentId: Long,
    val limits: SafetyLimitsResponse?,
    val effectiveLimits: SafetyLimitsResponse,
    val limitsSource: LimitsSource
) {
    private constructor(room: Room, view: LimitsView) : this(
        id = room.id!!,
        name = room.name,
        description = room.description,
        areaM2 = room.areaM2,
        environmentId = room.environment?.id ?: 0L,
        limits = view.limits,
        effectiveLimits = view.effectiveLimits,
        limitsSource = view.limitsSource
    )

    constructor(room: Room) : this(room, room.limitsView())
}

data class EnvironmentResponse(
    val id: Long,
    val name: String,
    val description: String,
    val limits: SafetyLimitsResponse?,
    val effectiveLimits: SafetyLimitsResponse,
    val limitsSource: LimitsSource,
    val rooms: List<RoomResponse>
) {
    private constructor(env: Environment, view: LimitsView) : this(
        id = env.id!!,
        name = env.name,
        description = env.description,
        limits = view.limits,
        effectiveLimits = view.effectiveLimits,
        limitsSource = view.limitsSource,
        rooms = env.rooms.map { RoomResponse(it) }
    )

    constructor(env: Environment) : this(env, env.limitsView())
}

/** Limites do ambiente: os próprios ou o padrão do sistema. */
fun Environment.limitsView(): LimitsView = SafetyLimitsRules.view(own = limits, environment = limits)

/** Limites do cômodo: os próprios, os do ambiente ou o padrão do sistema. */
fun Room.limitsView(): LimitsView =
    SafetyLimitsRules.view(own = limits, room = limits, environment = environment?.limits)
