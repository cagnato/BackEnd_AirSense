package br.pucpr.airsense.devices

import br.pucpr.airsense.environments.RoomRepository
import br.pucpr.airsense.exception.ConflictException
import br.pucpr.airsense.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val roomRepository: RoomRepository
) {
    private val log = LoggerFactory.getLogger(DeviceService::class.java)

    fun create(req: DeviceRequest): DeviceResponse {
        if (deviceRepository.existsBySerialNumber(req.serialNumber)) {
            throw ConflictException("Já existe um dispositivo IAQ cadastrado com o serial ${req.serialNumber}")
        }

        val device = Device(
            serialNumber = req.serialNumber,
            name = req.name,
            firmwareVersion = req.firmwareVersion,
            active = req.active
        )
        val saved = deviceRepository.save(device)
        log.info("Dispositivo IAQ cadastrado: id={}, serial={}", saved.id, saved.serialNumber)
        return DeviceResponse(saved)
    }

    fun findAll(): List<DeviceResponse> =
        deviceRepository.findAll().map { DeviceResponse(it) }

    fun findById(id: Long): DeviceResponse {
        val device = deviceRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Dispositivo com id=$id não encontrado")
        return DeviceResponse(device)
    }

    fun bindToRoom(deviceId: Long, roomId: Long): DeviceResponse {
        val device = deviceRepository.findByIdOrNull(deviceId)
            ?: throw NotFoundException("Dispositivo com id=$deviceId não encontrado")

        val room = roomRepository.findByIdOrNull(roomId)
            ?: throw NotFoundException("Cômodo com id=$roomId não encontrado")

        device.room = room
        val saved = deviceRepository.save(device)
        log.info("Dispositivo id={} vinculado ao cômodo id={}", deviceId, roomId)
        return DeviceResponse(saved)
    }

    fun unbindFromRoom(deviceId: Long): DeviceResponse {
        val device = deviceRepository.findByIdOrNull(deviceId)
            ?: throw NotFoundException("Dispositivo com id=$deviceId não encontrado")

        device.room = null
        val saved = deviceRepository.save(device)
        log.info("Dispositivo id={} desvinculado de qualquer cômodo", deviceId)
        return DeviceResponse(saved)
    }

    fun delete(id: Long) {
        if (!deviceRepository.existsById(id)) {
            throw NotFoundException("Dispositivo com id=$id não encontrado")
        }
        deviceRepository.deleteById(id)
        log.info("Dispositivo id={} excluído", id)
    }
}