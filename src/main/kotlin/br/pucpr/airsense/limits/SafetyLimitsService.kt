package br.pucpr.airsense.limits

import br.pucpr.airsense.devices.Device
import br.pucpr.airsense.devices.DeviceRepository
import br.pucpr.airsense.devices.limitsView
import br.pucpr.airsense.environments.Environment
import br.pucpr.airsense.environments.EnvironmentRepository
import br.pucpr.airsense.environments.Room
import br.pucpr.airsense.environments.RoomRepository
import br.pucpr.airsense.environments.limitsView
import br.pucpr.airsense.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Limites próprios de ambiente, cômodo e dispositivo (BK.10).
 * Cada um pode ter os seus; sem eles, vale o nível acima (dispositivo -> cômodo -> ambiente -> padrão).
 */
@Service
@Transactional
class SafetyLimitsService(
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository,
    private val deviceRepository: DeviceRepository
) {
    private val log = LoggerFactory.getLogger(SafetyLimitsService::class.java)

    // --- Ambiente ---

    @Transactional(readOnly = true)
    fun getForEnvironment(ownerId: Long, environmentId: Long): LimitsView =
        findEnvironment(ownerId, environmentId).limitsView()

    fun setForEnvironment(ownerId: Long, environmentId: Long, req: SafetyLimitsRequest): LimitsView {
        val env = findEnvironment(ownerId, environmentId)
        env.limits = SafetyLimitsRules.merge(env.limits, req, "do ambiente id=$environmentId")
        environmentRepository.save(env)
        log.info("Limites definidos no ambiente id={}", environmentId)
        return env.limitsView()
    }

    fun removeFromEnvironment(ownerId: Long, environmentId: Long) {
        val env = findEnvironment(ownerId, environmentId)
        if (env.limits == null) throw NotFoundException("O ambiente id=$environmentId não tem limites próprios")
        env.limits = null // orphanRemoval apaga os limites
        log.info("Limites removidos do ambiente id={}", environmentId)
    }

    // --- Cômodo ---

    @Transactional(readOnly = true)
    fun getForRoom(ownerId: Long, environmentId: Long, roomId: Long): LimitsView =
        findRoom(ownerId, environmentId, roomId).limitsView()

    fun setForRoom(ownerId: Long, environmentId: Long, roomId: Long, req: SafetyLimitsRequest): LimitsView {
        val room = findRoom(ownerId, environmentId, roomId)
        room.limits = SafetyLimitsRules.merge(room.limits, req, "do cômodo id=$roomId")
        roomRepository.save(room)
        log.info("Limites definidos no cômodo id={} (ambiente id={})", roomId, environmentId)
        return room.limitsView()
    }

    fun removeFromRoom(ownerId: Long, environmentId: Long, roomId: Long) {
        val room = findRoom(ownerId, environmentId, roomId)
        if (room.limits == null) throw NotFoundException("O cômodo id=$roomId não tem limites próprios")
        room.limits = null
        log.info("Limites removidos do cômodo id={} (ambiente id={})", roomId, environmentId)
    }

    // --- Dispositivo ---

    @Transactional(readOnly = true)
    fun getForDevice(ownerId: Long, deviceId: Long): LimitsView =
        findDevice(ownerId, deviceId).limitsView()

    fun setForDevice(ownerId: Long, deviceId: Long, req: SafetyLimitsRequest): LimitsView {
        val device = findDevice(ownerId, deviceId)
        device.limits = SafetyLimitsRules.merge(device.limits, req, "do dispositivo id=$deviceId")
        deviceRepository.save(device)
        log.info("Limites definidos no dispositivo id={}", deviceId)
        return device.limitsView()
    }

    fun removeFromDevice(ownerId: Long, deviceId: Long) {
        val device = findDevice(ownerId, deviceId)
        if (device.limits == null) throw NotFoundException("O dispositivo id=$deviceId não tem limites próprios")
        device.limits = null
        log.info("Limites removidos do dispositivo id={}", deviceId)
    }

    // --- Auxiliares (só encontram o que é do usuário) ---

    private fun findEnvironment(ownerId: Long, id: Long): Environment =
        environmentRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NotFoundException("Ambiente com id=$id não encontrado")

    private fun findRoom(ownerId: Long, environmentId: Long, roomId: Long): Room {
        val room = roomRepository.findByIdAndEnvironmentOwnerId(roomId, ownerId)
        if (room == null || room.environment?.id != environmentId) {
            throw NotFoundException("Cômodo id=$roomId não encontrado neste ambiente")
        }
        return room
    }

    private fun findDevice(ownerId: Long, id: Long): Device =
        deviceRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NotFoundException("Dispositivo com id=$id não encontrado")
}
