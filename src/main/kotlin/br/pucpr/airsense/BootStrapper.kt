package br.pucpr.airsense

import br.pucpr.airsense.users.Role
import br.pucpr.airsense.users.RoleRepository
import br.pucpr.airsense.users.User
import br.pucpr.airsense.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// Garante as roles USER e ADMIN e um administrador inicial ao subir a aplicação.
@Component
class BootStrapper(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${airsense.admin.email}") private val adminEmail: String,
    @Value("\${airsense.admin.password}") private val adminPassword: String
) : ApplicationListener<ContextRefreshedEvent> {

    private val log = LoggerFactory.getLogger(BootStrapper::class.java)

    @Transactional
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val userRole = roleRepository.findByIdOrNull(ROLE_USER)
            ?: roleRepository.save(Role(ROLE_USER, "Usuário padrão do AirSense"))
        val adminRole = roleRepository.findByIdOrNull(ROLE_ADMIN)
            ?: roleRepository.save(Role(ROLE_ADMIN, "Administrador do AirSense"))

        val email = adminEmail.trim().lowercase()
        if (userRepository.findByEmail(email) == null) {
            userRepository.save(
                User(
                    name = "Administrador",
                    email = email,
                    password = passwordEncoder.encode(adminPassword),
                    roles = mutableSetOf(userRole, adminRole)
                )
            )
            log.info("Administrador inicial criado: {}", email)
        }
    }

    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ADMIN = "ADMIN"
    }
}
