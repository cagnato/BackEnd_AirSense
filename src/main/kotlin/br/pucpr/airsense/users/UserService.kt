package br.pucpr.airsense.users

import br.pucpr.airsense.BootStrapper
import br.pucpr.airsense.devices.DeviceRepository
import br.pucpr.airsense.environments.EnvironmentRepository
import br.pucpr.airsense.exception.ApiException
import br.pucpr.airsense.exception.BadRequestException
import br.pucpr.airsense.exception.ConflictException
import br.pucpr.airsense.exception.LockedException
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.exception.UnauthorizedException
import br.pucpr.airsense.security.Jwt
import br.pucpr.airsense.security.UserToken
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

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

    companion object {
        private const val EMAIL_CONFLICT = "E-mail já cadastrado."
        private const val INVALID_CREDENTIALS = "E-mail ou senha inválidos."
        private const val ACCOUNT_LOCKED =
            "Acesso bloqueado por 15 minutos após 5 tentativas inválidas. O bloqueio é temporário; tente novamente mais tarde."
        private const val MAX_FAILED_ATTEMPTS = 5
        private val LOCK_DURATION: Duration = Duration.ofMinutes(15)
    }

    fun create(req: CreateUserRequest): UserResponse {
        val email = normalizeEmail(req.email)
        if (userRepository.existsByEmail(email)) {
            throw ConflictException(EMAIL_CONFLICT)
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

    // Sem rollback nas recusas: o contador de tentativas precisa ser gravado mesmo com o erro.
    @Transactional(noRollbackFor = [ApiException::class])
    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(normalizeEmail(req.username))
            ?: throw UnauthorizedException(INVALID_CREDENTIALS)

        val now = Instant.now()
        val lockedUntil = user.lockedUntil
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            log.warn("Login recusado, conta bloqueada até {}: userId={}", lockedUntil, user.id)
            throw LockedException(ACCOUNT_LOCKED)
        }

        if (!passwordEncoder.matches(req.password, user.password)) {
            // Na 5ª senha errada seguida, bloqueia por 15 minutos e zera o contador.
            val attempts = user.failedLoginAttempts + 1
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.failedLoginAttempts = 0
                user.lockedUntil = now.plus(LOCK_DURATION)
                userRepository.save(user)
                log.warn("Conta bloqueada por {} tentativas inválidas: userId={}", MAX_FAILED_ATTEMPTS, user.id)
                throw LockedException(ACCOUNT_LOCKED)
            }
            user.failedLoginAttempts = attempts
            userRepository.save(user)
            log.warn("Tentativa de login inválida ({}/{}) para o email={}", attempts, MAX_FAILED_ATTEMPTS, req.username)
            throw UnauthorizedException(INVALID_CREDENTIALS)
        }

        if (user.failedLoginAttempts > 0 || user.lockedUntil != null) {
            user.failedLoginAttempts = 0
            user.lockedUntil = null
            userRepository.save(user)
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
        req.email?.let { normalizeEmail(it) }?.let { email ->
            if (userRepository.existsByEmailAndIdNot(email, id)) {
                throw ConflictException(EMAIL_CONFLICT)
            }
            user.email = email
        }
        user.name = req.name.trim()
        val saved = userRepository.save(user)
        log.info("Usuário atualizado: id={}", saved.id)
        return UserResponse(saved)
    }

    fun changePassword(id: Long, req: ChangePasswordRequest, requester: UserToken) {
        val user = findAccessible(id, requester)
        if (!passwordEncoder.matches(req.currentPassword, user.password)) {
            throw BadRequestException("Senha atual incorreta")
        }
        user.password = passwordEncoder.encode(req.newPassword)
        userRepository.save(user)
        log.info("Senha alterada: userId={}", id)
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
