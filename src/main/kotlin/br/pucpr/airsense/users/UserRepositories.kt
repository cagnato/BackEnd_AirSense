package br.pucpr.airsense.users

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
    fun countByRolesName(roleName: String): Long
}

@Repository
interface RoleRepository : JpaRepository<Role, String>