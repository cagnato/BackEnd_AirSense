package br.pucpr.airsense.devices

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {

    fun existsByDeviceId(deviceId: String): Boolean

    fun findByIdAndOwnerId(id: Long, ownerId: Long): Device?

    fun findAllByOwnerId(ownerId: Long): List<Device>

    fun findAllByRoomId(roomId: Long): List<Device>

    fun findAllByEnvironmentId(environmentId: Long): List<Device>

    @Query("""
        SELECT d FROM Device d
        LEFT JOIN d.room r
        LEFT JOIN d.environment e
        WHERE d.owner.id = :ownerId
          AND (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:status IS NULL OR d.status = :status)
          AND (:roomId IS NULL OR r.id = :roomId)
          AND (:environmentId IS NULL OR e.id = :environmentId)
    """)
    fun findWithFilters(
        @Param("ownerId") ownerId: Long,
        @Param("name") name: String?,
        @Param("status") status: DeviceStatus?,
        @Param("roomId") roomId: Long?,
        @Param("environmentId") environmentId: Long?,
        sort: Sort
    ): List<Device>
}
