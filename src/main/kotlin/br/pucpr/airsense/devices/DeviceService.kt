package br.pucpr.airsense.devices

import br.pucpr.airsense.common.buildSort
import br.pucpr.airsense.environments.EnvironmentRepository
import br.pucpr.airsense.environments.RoomRepository
import br.pucpr.airsense.exception.BadRequestException
import br.pucpr.airsense.exception.ConflictException
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.limits.SafetyLimitsRules
import br.pucpr.airsense.users.UserRepository
import br.pucpr.airsense.reports.ReportRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository
) {
    private val log = LoggerFactory.getLogger(DeviceService::class.java)

    companion object {
        // Mesmas opções do SortOption do app: cadastro (id), nome, status e ambiente
        private val SORT_FIELDS = setOf("id", "name", "deviceId", "status", "environment")
        // Ordenado em memória: devices sem ambiente vão para o fim, como no app.
        private const val SORT_ENVIRONMENT = "environment"
        private const val DEVICE_ID_CONFLICT = "Dispositivo já cadastrado."

        fun generateDeviceId(): String =
            "DEV-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
    }

    fun create(ownerId: Long, req: DeviceRequest): DeviceResponse {
        val deviceId = req.deviceId?.trim().takeUnless { it.isNullOrEmpty() } ?: generateDeviceId()
        if (deviceRepository.existsByDeviceIdIgnoreCase(deviceId)) {
            throw ConflictException(DEVICE_ID_CONFLICT)
        }

        val device = Device(deviceId = deviceId, owner = userRepository.getReferenceById(ownerId))
        applyRequest(ownerId, device, req)
        val saved = deviceRepository.save(device)
        log.info("Dispositivo cadastrado: id={}, deviceId={}, owner={}", saved.id, saved.deviceId, ownerId)
        return DeviceResponse(saved)
    }

    @Transactional(readOnly = true)
    fun search(
        ownerId: Long,
        name: String?,
        status: DeviceStatus?,
        roomId: Long?,
        environmentId: Long?,
        sortBy: String,
        sortDir: String
    ): List<DeviceResponse> {
        val sort = buildSort(sortBy, sortDir, SORT_FIELDS)
        if (sortBy != SORT_ENVIRONMENT) {
            return deviceRepository.findWithFilters(ownerId, name, status, roomId, environmentId, sort)
                .map { DeviceResponse(it) }
        }

        val byName = compareBy<Device, String>(String.CASE_INSENSITIVE_ORDER) { it.environment!!.name }
        val withEnv = if (sort.first().isDescending) byName.reversed() else byName
        val comparator = compareBy<Device> { it.environment == null }
            .then { a, b -> if (a.environment == null || b.environment == null) 0 else withEnv.compare(a, b) }
            .thenBy { it.id }
        return deviceRepository.findWithFilters(ownerId, name, status, roomId, environmentId, Sort.by("id"))
            .sortedWith(comparator)
            .map { DeviceResponse(it) }
    }

    @Transactional(readOnly = true)
    fun findById(ownerId: Long, id: Long): DeviceResponse =
        DeviceResponse(findOwned(ownerId, id))

    // Substitui o device inteiro, como o app faz ao salvar (inclui ambiente e cômodo).
    fun update(ownerId: Long, id: Long, req: DeviceRequest): DeviceResponse {
        val device = findOwned(ownerId, id)

        // deviceId vazio no update mantém o atual
        val newDeviceId = req.deviceId?.trim().takeUnless { it.isNullOrEmpty() }
        if (newDeviceId != null && newDeviceId != device.deviceId) {
            if (deviceRepository.existsByDeviceIdIgnoreCaseAndIdNot(newDeviceId, id)) {
                throw ConflictException(DEVICE_ID_CONFLICT)
            }
            device.deviceId = newDeviceId
        }

        applyRequest(ownerId, device, req)
        val saved = deviceRepository.save(device)
        log.info("Dispositivo atualizado: id={}", saved.id)
        return DeviceResponse(saved)
    }

    fun bindToRoom(ownerId: Long, id: Long, roomId: Long): DeviceResponse {
        val device = findOwned(ownerId, id)
        val room = roomRepository.findByIdAndEnvironmentOwnerId(roomId, ownerId)
            ?: throw NotFoundException("Cômodo com id=$roomId não encontrado")

        device.room = room
        device.environment = room.environment
        val saved = deviceRepository.save(device)
        log.info("Dispositivo id={} vinculado ao cômodo id={}", id, roomId)
        return DeviceResponse(saved)
    }

    // Tira o device do cômodo, mas ele continua no ambiente.
    fun unbindFromRoom(ownerId: Long, id: Long): DeviceResponse {
        val device = findOwned(ownerId, id)
        device.room = null
        val saved = deviceRepository.save(device)
        log.info("Dispositivo id={} desvinculado de qualquer cômodo", id)
        return DeviceResponse(saved)
    }

    fun delete(ownerId: Long, id: Long) {
        val device = findOwned(ownerId, id)
        reportRepository.unlinkDevice(id) // D33: relatórios mantêm o nome, sem o vínculo
        deviceRepository.delete(device)
        log.info("Dispositivo id={} excluído", id)
    }

    // --- Auxiliares ---

    private fun findOwned(ownerId: Long, id: Long): Device =
        deviceRepository.findByIdAndOwnerId(id, ownerId)
            ?: throw NotFoundException("Dispositivo com id=$id não encontrado")

    private fun applyRequest(ownerId: Long, device: Device, req: DeviceRequest) {
        device.name = req.name.trim()
        device.status = req.status
        // PUT substitui o device inteiro: limits nulo remove os limites próprios (passa a herdar).
        device.limits = SafetyLimitsRules.merge(device.limits, req.limits, "do dispositivo ${device.deviceId}")
        applyLocation(ownerId, device, req.environmentId, req.roomId)
    }

    private fun applyLocation(ownerId: Long, device: Device, environmentId: Long?, roomId: Long?) {
        if (roomId != null) {
            val room = roomRepository.findByIdAndEnvironmentOwnerId(roomId, ownerId)
                ?: throw NotFoundException("Cômodo com id=$roomId não encontrado")
            if (environmentId != null && room.environment?.id != environmentId) {
                throw BadRequestException("O cômodo id=$roomId não pertence ao ambiente id=$environmentId")
            }
            device.room = room
            device.environment = room.environment
            return
        }
        device.room = null
        device.environment = environmentId?.let {
            environmentRepository.findByIdAndOwnerId(it, ownerId)
                ?: throw NotFoundException("Ambiente com id=$it não encontrado")
        }
    }
}
