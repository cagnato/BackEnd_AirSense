package br.pucpr.airsense.users

import br.pucpr.airsense.exception.ConflictException
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.exception.UnauthorizedException
import br.pucpr.airsense.security.Jwt
import br.pucpr.airsense.security.UserToken
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwt: Jwt
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun create(req: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw ConflictException("Já existe um usuário cadastrado com o e-mail ${req.email}")
        }

        val defaultRole = roleRepository.findByIdOrNull("USER")
            ?: roleRepository.save(Role("USER", "Usuário padrão do AirSense"))

        val user = User(
            name = req.name,
            email = req.email,
            password = passwordEncoder.encode(req.password),
            roles = mutableSetOf(defaultRole)
        )

        val saved = userRepository.save(user)
        log.info("Usuário criado com sucesso: id={}, email={}", saved.id, saved.email)
        return UserResponse(saved)
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(req.username)
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

    fun findAll(): List<UserResponse> =
        userRepository.findAll().map { UserResponse(it) }

    fun findById(id: Long): UserResponse {
        val user = userRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Usuário com id=$id não encontrado")
        return UserResponse(user)
    }

    fun update(id: Long, req: UpdateUserRequest): UserResponse {
        val user = userRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Usuário com id=$id não encontrado")
        user.name = req.name
        val saved = userRepository.save(user)
        log.info("Usuário atualizado: id={}", saved.id)
        return UserResponse(saved)
    }

    fun delete(id: Long) {
        if (!userRepository.existsById(id)) {
            throw NotFoundException("Usuário com id=$id não encontrado")
        }
        userRepository.deleteById(id)
        log.info("Usuário removido: id={}", id)
    }

    fun addRole(userId: Long, roleName: String): UserResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException("Usuário com id=$userId não encontrado")

        val normalizedRole = roleName.uppercase()
        val role = roleRepository.findByIdOrNull(normalizedRole)
            ?: roleRepository.save(Role(normalizedRole, "Role $normalizedRole"))

        user.roles.add(role)
        val saved = userRepository.save(user)
        log.info("Role {} associada ao usuário id={}", normalizedRole, userId)
        return UserResponse(saved)
    }
}