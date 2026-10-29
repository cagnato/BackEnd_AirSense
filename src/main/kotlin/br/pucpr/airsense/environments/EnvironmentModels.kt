package br.pucpr.airsense.environments

import br.pucpr.airsense.users.User
import jakarta.persistence.*
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

    @Column(nullable = false)
    var description: String = "",

    @Column(nullable = false)
    var city: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User? = null,

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
    var areaM2: Double = 0.0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    var environment: Environment? = null
)

// --- DTOs ---

data class EnvironmentRequest(
    @field:NotBlank(message = "O nome do ambiente é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    @field:NotBlank(message = "A descrição é obrigatória")
    @field:Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    val description: String,

    @field:NotBlank(message = "A cidade é obrigatória")
    @field:Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
    val city: String
)

data class RoomRequest(
    @field:NotBlank(message = "O nome do cômodo é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    @field:Positive(message = "A área em m² deve ser maior que zero")
    @field:DecimalMax(value = "100000.0", message = "A área deve ser de no máximo 100000 m²")
    val areaM2: Double
)

data class RoomResponse(
    val id: Long,
    val name: String,
    val areaM2: Double,
    val environmentId: Long
) {
    constructor(room: Room) : this(
        id = room.id!!,
        name = room.name,
        areaM2 = room.areaM2,
        environmentId = room.environment?.id ?: 0L
    )
}

data class EnvironmentResponse(
    val id: Long,
    val name: String,
    val description: String,
    val city: String,
    val rooms: List<RoomResponse>
) {
    constructor(env: Environment) : this(
        id = env.id!!,
        name = env.name,
        description = env.description,
        city = env.city,
        rooms = env.rooms.map { RoomResponse(it) }
    )
}
