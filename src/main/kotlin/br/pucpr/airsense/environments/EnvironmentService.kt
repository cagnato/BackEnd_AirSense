package br.pucpr.airsense.environments

import br.pucpr.airsense.common.buildSort
import br.pucpr.airsense.devices.DeviceRepository
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EnvironmentService(
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository,
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(EnvironmentService::class.java)

    companion object {
        private val SORT_FIELDS = setOf("id", "name", "city")
    }

    fun create(ownerId: Long, req: EnvironmentRequest): EnvironmentResponse {
        val env = Environment(
            name = req.name,
            description = req.description,
            city = req.city,
            owner = userRepository.getReferenceById(ownerId)
        )
        val saved = environmentRepository.save(env)
        log.info("Ambiente Macro criado: id={}, name={}, owner={}", saved.id, saved.name, ownerId)
        return EnvironmentResponse(saved)
    }

    @Transactional(readOnly = true)
    fun search(ownerId: Long, name: String?, city: String?, sortBy: String, sortDir: String): List<EnvironmentResponse> {
        val sort = buildSort(sortBy, sortDir, SORT_FIELDS)
        return environmentRepository.findWithFilters(ownerId, name, city, sort)
            .map { EnvironmentResponse(it) }
    }

    @Transactional(readOnly = true)
    fun findById(ownerId: Long, id: Long): EnvironmentResponse =
        EnvironmentResponse(findOwned(ownerId, id))

    fun update(ownerId: Long, id: Long, req: EnvironmentRequest): EnvironmentResponse {
        val env = findOwned(ownerId, id)
        env.name = req.name
        env.description = req.description
        env.city = req.city

        val saved = environmentRepository.save(env)
        log.info("Ambiente atualizado: id={}", saved.id)
        return EnvironmentResponse(saved)
    }

    fun delete(ownerId: Long, id: Long) {
        val env = findOwned(ownerId, id)
        // Devices deste ambiente não são apagados, só ficam sem ambiente e sem room
        env.rooms.forEach { room -> unbindDevices(room.id!!) }
        deviceRepository.findAllByEnvironmentId(id).forEach {
            it.room = null
            it.environment = null
        }
        environmentRepository.delete(env)
        log.info("Ambiente removido: id={}", id)
    }

    // --- Cômodos (Rooms) ---

    fun addRoom(ownerId: Long, environmentId: Long, req: RoomRequest): RoomResponse {
        val env = findOwned(ownerId, environmentId)
        val room = roomRepository.save(Room(name = req.name, areaM2 = req.areaM2, environment = env))
        env.rooms.add(room)
        log.info("Cômodo '{}' adicionado ao ambiente id={}", req.name, environmentId)
        return RoomResponse(room)
    }

    @Transactional(readOnly = true)
    fun listRooms(ownerId: Long, environmentId: Long): List<RoomResponse> {
        findOwned(ownerId, environmentId)
        return roomRepository.findAllByEnvironmentId(environmentId).map { RoomResponse(it) }
    }

    fun updateRoom(ownerId: Long, environmentId: Long, roomId: Long, req: RoomRequest): RoomResponse {
        val room = findOwnedRoom(ownerId, environmentId, roomId)
        room.name = req.name
        room.areaM2 = req.areaM2
        val saved = roomRepository.save(room)
        log.info("Cômodo id={} atualizado no ambiente id={}", roomId, environmentId)
        return RoomResponse(saved)
    }

    fun removeRoom(ownerId: Long, environmentId: Long, roomId: Long) {
        val env = findOwned(ownerId, environmentId)
        val room = env.rooms.find { it.id == roomId }
            ?: throw NotFoundException("Cômodo id=$roomId não encontrado neste ambiente")
        unbindDevices(roomId) // continuam no ambiente, só sem room
        env.rooms.remove(room) // orphanRemoval apaga o cômodo
        log.info("Cômodo id={} removido do ambiente id={}", roomId, environmentId)
    }

    // --- Auxiliares ---

    private fun findOwned(ownerId: Long, id: Long): Environment =
        environmentRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NotFoundException("Ambiente com id=$id não encontrado")

    private fun findOwnedRoom(ownerId: Long, environmentId: Long, roomId: Long): Room {
        val room = roomRepository.findByIdAndEnvironmentOwnerId(roomId, ownerId)
        if (room == null || room.environment?.id != environmentId) {
            throw NotFoundException("Cômodo id=$roomId não encontrado neste ambiente")
        }
        return room
    }

    private fun unbindDevices(roomId: Long) {
        deviceRepository.findAllByRoomId(roomId).forEach { it.room = null }
    }
}
