package br.pucpr.airsense.environments

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EnvironmentRepository : JpaRepository<Environment, Long> {

    @Query("""
        SELECT e FROM Environment e
        WHERE e.owner.id = :ownerId
          AND (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%')))
    """)
    fun findWithFilters(
        @Param("ownerId") ownerId: Long,
        @Param("name") name: String?,
        @Param("city") city: String?,
        sort: Sort
    ): List<Environment>

    fun findByIdAndOwnerId(id: Long, ownerId: Long): Environment?

    fun findAllByOwnerId(ownerId: Long): List<Environment>
}

@Repository
interface RoomRepository : JpaRepository<Room, Long> {

    fun findAllByEnvironmentId(environmentId: Long): List<Room>

    // Busca o cômodo garantindo que ele pertence a um ambiente do dono informado.
    fun findByIdAndEnvironmentOwnerId(id: Long, ownerId: Long): Room?
}
