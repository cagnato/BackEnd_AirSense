package br.pucpr.airsense.users

import br.pucpr.airsense.common.TrimmedStringDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import java.time.Instant
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
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

    // Senhas erradas seguidas e fim do bloqueio temporário do login (nulo = sem bloqueio)
    @Column(nullable = false)
    @ColumnDefault("0")
    var failedLoginAttempts: Int = 0,

    var lockedUntil: Instant? = null,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "tbl_user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_name")]
    )
    val roles: MutableSet<Role> = mutableSetOf()
)

// --- DTOs ---

// Regra de senha usada no cadastro e na troca de senha
object PasswordRule {
    const val UPPERCASE = "(?s).*\\p{Lu}.*"
    const val LOWERCASE = "(?s).*\\p{Ll}.*"
    const val DIGIT = "(?s).*\\p{Nd}.*"
    const val SPECIAL = "(?s).*[^\\p{L}\\p{Nd}].*"

    const val UPPERCASE_MESSAGE = "A senha deve ter pelo menos uma letra maiúscula"
    const val LOWERCASE_MESSAGE = "A senha deve ter pelo menos uma letra minúscula"
    const val DIGIT_MESSAGE = "A senha deve ter pelo menos um número"
    const val SPECIAL_MESSAGE = "A senha deve ter pelo menos um caractere especial (ex.: @, #, !)"
}

data class CreateUserRequest(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    @field:Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    @field:Email(message = "E-mail inválido")
    @field:NotBlank(message = "O e-mail é obrigatório")
    @JsonDeserialize(using = TrimmedStringDeserializer::class)
    val email: String,

    // Mesma regra do app: 8 a 72 caracteres (o BCrypt só considera 72 bytes),
    // com maiúscula, minúscula, número e caractere especial.
    @field:NotBlank(message = "A senha é obrigatória")
    @field:Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @field:Pattern(regexp = PasswordRule.UPPERCASE, message = PasswordRule.UPPERCASE_MESSAGE)
    @field:Pattern(regexp = PasswordRule.LOWERCASE, message = PasswordRule.LOWERCASE_MESSAGE)
    @field:Pattern(regexp = PasswordRule.DIGIT, message = PasswordRule.DIGIT_MESSAGE)
    @field:Pattern(regexp = PasswordRule.SPECIAL, message = PasswordRule.SPECIAL_MESSAGE)
    val password: String
)

data class UpdateUserRequest(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    val name: String,

    // Opcional: se não vier, o e-mail atual é mantido
    @field:Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    @field:Email(message = "E-mail inválido")
    @field:Pattern(regexp = ".*\\S.*", message = "O e-mail não pode ser vazio")
    @JsonDeserialize(using = TrimmedStringDeserializer::class)
    val email: String? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "A senha atual é obrigatória")
    val currentPassword: String,

    // Mesma regra da senha do cadastro
    @field:NotBlank(message = "A nova senha é obrigatória")
    @field:Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    @field:Pattern(regexp = PasswordRule.UPPERCASE, message = PasswordRule.UPPERCASE_MESSAGE)
    @field:Pattern(regexp = PasswordRule.LOWERCASE, message = PasswordRule.LOWERCASE_MESSAGE)
    @field:Pattern(regexp = PasswordRule.DIGIT, message = PasswordRule.DIGIT_MESSAGE)
    @field:Pattern(regexp = PasswordRule.SPECIAL, message = PasswordRule.SPECIAL_MESSAGE)
    val newPassword: String
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