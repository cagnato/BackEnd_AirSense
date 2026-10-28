package br.pucpr.airsense.devices

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {
    fun existsBySerialNumber(serialNumber: String): Boolean
}