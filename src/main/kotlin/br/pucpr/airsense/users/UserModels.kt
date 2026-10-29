package br.pucpr.airsense.users

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Entity
@Table(name = "tbl_roles")
class Role(
    @Id
    val name: String = "",
    val description: String = ""
)

@Entity
@Table(name = "tbl_users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Column(nullable = false)
    var name: String = "",

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tbl_user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_name")]
    )
    val roles: MutableSet<Role> = mutableSetOf()
)

// --- DTOs ---

data class CreateUserRequest(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    @field:Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    @field:Email(message = "E-mail inválido")
    @field:NotBlank(message = "O e-mail é obrigatório")
    val email: String,

    // BCrypt só considera os primeiros 72 bytes da senha
    @field:NotBlank(message = "A senha é obrigatória")
    @field:Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    val password: String
)

data class UpdateUserRequest(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String
)

data class LoginRequest(
    @field:NotBlank(message = "O e-mail é obrigatório") val username: String,
    @field:NotBlank(message = "A senha é obrigatória") val password: String
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val roles: Set<String>
) {
    constructor(user: User) : this(
        id = user.id!!,
        name = user.name,
        email = user.email,
        roles = user.roles.map { it.name }.toSet()
    )
}

data class LoginResponse(
    val token: String,
    val user: UserResponse
)