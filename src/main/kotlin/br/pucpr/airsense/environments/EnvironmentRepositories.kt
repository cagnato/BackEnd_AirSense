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
        WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:city IS NULL OR LOWER(e.city) LIKE LOWER(CONCAT('%', :city, '%')))
    """)
    fun findWithFilters(
        @Param("name") name: String?,
        @Param("city") city: String?,
        sort: Sort
    ): List<Environment>
}

@Repository
interface RoomRepository : JpaRepository<Room, Long>