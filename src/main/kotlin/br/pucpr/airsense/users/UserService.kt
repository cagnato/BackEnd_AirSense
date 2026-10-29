package br.pucpr.airsense.users

import br.pucpr.airsense.BootStrapper
import br.pucpr.airsense.devices.DeviceRepository
import br.pucpr.airsense.environments.EnvironmentRepository
import br.pucpr.airsense.exception.ConflictException
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.exception.UnauthorizedException
import br.pucpr.airsense.security.Jwt
import br.pucpr.airsense.security.UserToken
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val environmentRepository: EnvironmentRepository,
    private val deviceRepository: DeviceRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwt: Jwt
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun create(req: CreateUserRequest): UserResponse {
        val email = normalizeEmail(req.email)
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("Já existe um usuário cadastrado com o e-mail $email")
        }

        val defaultRole = roleRepository.findByIdOrNull(BootStrapper.ROLE_USER)
            ?: roleRepository.save(Role(BootStrapper.ROLE_USER, "Usuário padrão do AirSense"))

        val user = User(
            name = req.name.trim(),
            email = email,
            password = passwordEncoder.encode(req.password),
            roles = mutableSetOf(defaultRole)
        )

        val saved = userRepository.save(user)
        log.info("Usuário criado com sucesso: id={}, email={}", saved.id, saved.email)
        return UserResponse(saved)
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(normalizeEmail(req.username))
            ?: throw UnauthorizedException("Usuário ou senha inválidos")

        if (!passwordEncoder.matches(req.password, user.password)) {
            log.warn("Tentativa de login inválida para o email={}", req.username)
            throw UnauthorizedException("Usuário ou senha inválidos")
        }

        val userToken = UserToken(
            id = user.id!!,
            login = user.email,
            name = user.name,
            roles = user.roles.map { it.name }.toSet()
        )

        val token = jwt.createToken(userToken)
        log.info("Login realizado com sucesso: userId={}", user.id)
        return LoginResponse(token = token, user = UserResponse(user))
    }

    @Transactional(readOnly = true)
    fun findAll(): List<UserResponse> =
        userRepository.findAll().map { UserResponse(it) }

    @Transactional(readOnly = true)
    fun findById(id: Long, requester: UserToken): UserResponse =
        UserResponse(findAccessible(id, requester))

    fun update(id: Long, req: UpdateUserRequest, requester: UserToken): UserResponse {
        val user = findAccessible(id, requester)
        user.name = req.name.trim()
        val saved = userRepository.save(user)
        log.info("Usuário atualizado: id={}", saved.id)
        return UserResponse(saved)
    }

    fun delete(id: Long, requester: UserToken) {
        val user = findAccessible(id, requester)
        val isAdmin = user.roles.any { it.name == BootStrapper.ROLE_ADMIN }
        if (isAdmin && userRepository.countByRolesName(BootStrapper.ROLE_ADMIN) <= 1) {
            throw ConflictException("Não é possível remover o último administrador do sistema")
        }
        // Remove primeiro o que pertence ao usuário, para não violar as FKs de owner
        deviceRepository.deleteAll(deviceRepository.findAllByOwnerId(id))
        environmentRepository.deleteAll(environmentRepository.findAllByOwnerId(id))
        userRepository.delete(user)
        log.info("Usuário removido: id={}", id)
    }

    fun addRole(userId: Long, roleName: String): UserResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException("Usuário com id=$userId não encontrado")

        val normalizedRole = roleName.uppercase()
        // Só aceita roles que já existem (criadas pelo BootStrapper)
        val role = roleRepository.findByIdOrNull(normalizedRole)
            ?: throw NotFoundException(
                "Role $normalizedRole não existe. Use: ${roleRepository.findAll().map { it.name }}"
            )

        user.roles.add(role)
        val saved = userRepository.save(user)
        log.info("Role {} associada ao usuário id={}", normalizedRole, userId)
        return UserResponse(saved)
    }

    // E-mail sem espaços e em minúsculas, para "A@X.com" e "a@x.com" serem a mesma conta.
    private fun normalizeEmail(email: String) = email.trim().lowercase()

    // Usuário comum só acessa a si mesmo; outro id responde 404. ADMIN acessa qualquer um.
    private fun findAccessible(id: Long, requester: UserToken): User {
        val isAdmin = BootStrapper.ROLE_ADMIN in requester.roles.map { it.uppercase() }
        if (!isAdmin && id != requester.id) {
            throw NotFoundException("Usuário com id=$id não encontrado")
        }
        return userRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Usuário com id=$id não encontrado")
    }
}
