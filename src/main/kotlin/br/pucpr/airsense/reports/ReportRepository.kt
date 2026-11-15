package br.pucpr.airsense.reports

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
interface ReportRepository : JpaRepository<Report, Long> {

    fun findByIdAndOwnerId(id: Long, ownerId: Long): Report?

    @Query("""
        SELECT r FROM Report r
        WHERE r.owner.id = :ownerId
          AND (:startDate IS NULL OR r.startDate >= :startDate)
          AND (:endDate IS NULL OR r.endDate <= :endDate)
          AND (:environmentId IS NULL OR r.environment.id = :environmentId)
          AND (:roomId IS NULL OR r.room.id = :roomId)
          AND (:deviceId IS NULL OR r.device.id = :deviceId)
    """)
    fun findWithFilters(
        @Param("ownerId") ownerId: Long,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        @Param("environmentId") environmentId: Long?,
        @Param("roomId") roomId: Long?,
        @Param("deviceId") deviceId: Long?,
        sort: Sort
    ): List<Report>

    // --- D33: Deixar o vínculo nulo nos relatórios ao excluir dispositivo/cômodo/ambiente ---
    @Transactional
    @Modifying
    @Query("UPDATE Report r SET r.device = null WHERE r.device.id = :deviceId")
    fun unlinkDevice(@Param("deviceId") deviceId: Long)

    @Transactional
    @Modifying
    @Query("UPDATE Report r SET r.room = null WHERE r.room.id = :roomId")
    fun unlinkRoom(@Param("roomId") roomId: Long)

    @Transactional
    @Modifying
    @Query("UPDATE Report r SET r.environment = null WHERE r.environment.id = :environmentId")
    fun unlinkEnvironment(@Param("environmentId") environmentId: Long)

    // --- Ao excluir usuário: apagar todos os relatórios dele antes ---
    @Transactional
    @Modifying
    @Query("DELETE FROM Report r WHERE r.owner.id = :ownerId")
    fun deleteAllByOwnerId(@Param("ownerId") ownerId: Long)
}