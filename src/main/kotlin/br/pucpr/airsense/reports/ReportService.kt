package br.pucpr.airsense.reports

import br.pucpr.airsense.common.buildSort
import br.pucpr.airsense.devices.Device
import br.pucpr.airsense.devices.DeviceRepository
import br.pucpr.airsense.environments.Environment
import br.pucpr.airsense.environments.EnvironmentRepository
import br.pucpr.airsense.environments.Room
import br.pucpr.airsense.environments.RoomRepository
import br.pucpr.airsense.exception.BadRequestException
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.users.User
import br.pucpr.airsense.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository,
    private val deviceRepository: DeviceRepository
) {
    private val log = LoggerFactory.getLogger(ReportService::class.java)

    companion object {
        private val ALLOWED_SORT_FIELDS = setOf("id", "startDate", "endDate", "createdAt")
    }

    @Transactional
    fun create(ownerId: Long, req: ReportRequest): ReportResponse {
        val owner = findOwnerOrThrow(ownerId)
        val startDate = req.startDate!!
        val endDate = req.endDate!!

        validatePeriod(startDate, endDate)

        val environment = req.environmentId?.let { findOwnedEnvironmentOrThrow(it, ownerId) }
        val room = req.roomId?.let { findOwnedRoomOrThrow(it, ownerId) }
        val device = req.deviceId?.let { findOwnedDeviceOrThrow(it, ownerId) }

        validateHierarchyConsistency(environment, room, device)

        val report = Report(
            owner = owner,
            environment = environment,
            room = room,
            device = device,
            startDate = startDate,
            endDate = endDate,
            createdAt = Instant.now(),
            environmentName = environment?.name,
            roomName = room?.name,
            deviceName = device?.name
        )

        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info(
            "Relatório criado: id={}, ownerId={}, startDate={}, endDate={}, envId={}, roomId={}, deviceId={}",
            saved.id, ownerId, startDate, endDate, environment?.id, room?.id, device?.id
        )
        return ReportResponse(saved)
    }

    fun findById(ownerId: Long, id: Long): ReportResponse {
        val report = findOwnedReportOrThrow(id, ownerId)
        return ReportResponse(report)
    }

    fun search(
        ownerId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?,
        environmentId: Long?,
        roomId: Long?,
        deviceId: Long?,
        sortBy: String,
        sortDir: String
    ): List<ReportResponse> {
        if (startDate != null && endDate != null) {
            validatePeriod(startDate, endDate)
        }

        val sort = buildSort(sortBy, sortDir, ALLOWED_SORT_FIELDS)
        return reportRepository.findWithFilters(
            ownerId = ownerId,
            startDate = startDate,
            endDate = endDate,
            environmentId = environmentId,
            roomId = roomId,
            deviceId = deviceId,
            sort = sort
        ).map { ReportResponse(it) }
    }

    @Transactional
    fun update(ownerId: Long, id: Long, req: ReportRequest): ReportResponse {
        val report = findOwnedReportOrThrow(id, ownerId)
        val startDate = req.startDate!!
        val endDate = req.endDate!!

        validatePeriod(startDate, endDate)

        val environment = req.environmentId?.let { findOwnedEnvironmentOrThrow(it, ownerId) }
        val room = req.roomId?.let { findOwnedRoomOrThrow(it, ownerId) }
        val device = req.deviceId?.let { findOwnedDeviceOrThrow(it, ownerId) }

        validateHierarchyConsistency(environment, room, device)

        report.startDate = startDate
        report.endDate = endDate
        report.environment = environment
        report.room = room
        report.device = device

        report.environmentName = environment?.name
        report.roomName = room?.name
        report.deviceName = device?.name

        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Relatório alterado: id={}, ownerId={}", saved.id, ownerId)
        return ReportResponse(saved)
    }

    @Transactional
    fun delete(ownerId: Long, id: Long) {
        val report = findOwnedReportOrThrow(id, ownerId)
        reportRepository.delete(report)
        log.info("Relatório excluído: id={}, ownerId={}", id, ownerId)
    }

    // --- Endpoints de Associação: Device ---

    @Transactional
    fun associateDevice(ownerId: Long, reportId: Long, deviceId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        val device = findOwnedDeviceOrThrow(deviceId, ownerId)

        validateHierarchyConsistency(report.environment, report.room, device)

        report.device = device
        report.deviceName = device.name
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Dispositivo id={} associado ao relatório id={}", deviceId, reportId)
        return ReportResponse(saved)
    }

    @Transactional
    fun disassociateDevice(ownerId: Long, reportId: Long, deviceId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        findOwnedDeviceOrThrow(deviceId, ownerId)

        if (report.device?.id != deviceId) {
            log.warn("Recusa ao desassociar: dispositivo id={} não está associado ao relatório id={}", deviceId, reportId)
            throw BadRequestException("O dispositivo informado não está associado a este relatório.")
        }

        report.device = null
        report.deviceName = null
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Dispositivo id={} desassociado do relatório id={}", deviceId, reportId)
        return ReportResponse(saved)
    }

    // --- Endpoints de Associação: Room ---

    @Transactional
    fun associateRoom(ownerId: Long, reportId: Long, roomId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        val room = findOwnedRoomOrThrow(roomId, ownerId)

        validateHierarchyConsistency(report.environment, room, report.device)

        report.room = room
        report.roomName = room.name
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Cômodo id={} associado ao relatório id={}", roomId, reportId)
        return ReportResponse(saved)
    }

    @Transactional
    fun disassociateRoom(ownerId: Long, reportId: Long, roomId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        findOwnedRoomOrThrow(roomId, ownerId)

        if (report.room?.id != roomId) {
            log.warn("Recusa ao desassociar: cômodo id={} não está associado ao relatório id={}", roomId, reportId)
            throw BadRequestException("O cômodo informado não está associado a este relatório.")
        }

        report.room = null
        report.roomName = null
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Cômodo id={} desassociado do relatório id={}", roomId, reportId)
        return ReportResponse(saved)
    }

    // --- Endpoints de Associação: Environment ---

    @Transactional
    fun associateEnvironment(ownerId: Long, reportId: Long, environmentId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        val environment = findOwnedEnvironmentOrThrow(environmentId, ownerId)

        validateHierarchyConsistency(environment, report.room, report.device)

        report.environment = environment
        report.environmentName = environment.name
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Ambiente id={} associado ao relatório id={}", environmentId, reportId)
        return ReportResponse(saved)
    }

    @Transactional
    fun disassociateEnvironment(ownerId: Long, reportId: Long, environmentId: Long): ReportResponse {
        val report = findOwnedReportOrThrow(reportId, ownerId)
        findOwnedEnvironmentOrThrow(environmentId, ownerId)

        if (report.environment?.id != environmentId) {
            log.warn("Recusa ao desassociar: ambiente id={} não está associado ao relatório id={}", environmentId, reportId)
            throw BadRequestException("O ambiente informado não está associado a este relatório.")
        }

        report.environment = null
        report.environmentName = null
        populateReportSummary(report)

        val saved = reportRepository.save(report)
        log.info("Ambiente id={} desassociado do relatório id={}", environmentId, reportId)
        return ReportResponse(saved)
    }

    // --- Validações Internas e Cálculo de Resumo (D32) ---

    private fun validatePeriod(startDate: LocalDate, endDate: LocalDate) {
        if (endDate.isBefore(startDate)) {
            log.warn("Recusa por período inválido: startDate={}, endDate={}", startDate, endDate)
            throw BadRequestException("Período inválido.")
        }
    }

    private fun validateHierarchyConsistency(
        environment: Environment?,
        room: Room?,
        device: Device?
    ) {
        // 1. Cômodo informado precisa ser do ambiente informado -> 400
        if (environment != null && room != null && room.environment?.id != environment.id) {
            log.warn(
                "Recusa: cômodo id={} pertence ao ambiente id={}, mas foi informado ambiente id={}",
                room.id, room.environment?.id, environment.id
            )
            throw BadRequestException("O cômodo informado não pertence ao ambiente informado.")
        }

        // 2. Dispositivo informado precisa estar no cômodo informado -> 400
        if (room != null && device != null && device.room?.id != room.id) {
            log.warn(
                "Recusa: dispositivo id={} está no cômodo id={}, mas foi informado cômodo id={}",
                device.id, device.room?.id, room.id
            )
            throw BadRequestException("O dispositivo informado não está no cômodo informado.")
        }

        // 3. Dispositivo informado precisa estar no ambiente informado -> 400
        if (environment != null && device != null) {
            val deviceEnvId = device.room?.environment?.id
            if (deviceEnvId != environment.id) {
                log.warn(
                    "Recusa: dispositivo id={} está no ambiente id={}, mas foi informado ambiente id={}",
                    device.id, deviceEnvId, environment.id
                )
                throw BadRequestException("O dispositivo informado não está no ambiente informado.")
            }
        }
    }

    private fun findOwnerOrThrow(ownerId: Long): User =
        userRepository.findByIdOrNull(ownerId) ?: run {
            log.warn("Usuário autenticado id={} não encontrado no banco", ownerId)
            throw NotFoundException("Usuário não encontrado.")
        }

    private fun findOwnedReportOrThrow(reportId: Long, ownerId: Long): Report =
        reportRepository.findByIdAndOwnerId(reportId, ownerId) ?: run {
            log.warn("Recusa: relatório id={} não encontrado para o usuário id={}", reportId, ownerId)
            throw NotFoundException("Relatório não encontrado.")
        }

    private fun findOwnedEnvironmentOrThrow(environmentId: Long, ownerId: Long): Environment {
        val env = environmentRepository.findByIdOrNull(environmentId)
        if (env == null || env.owner?.id != ownerId) {
            log.warn("Recusa: ambiente id={} não encontrado ou não pertence ao usuário id={}", environmentId, ownerId)
            throw NotFoundException("Ambiente não encontrado.")
        }
        return env
    }

    private fun findOwnedRoomOrThrow(roomId: Long, ownerId: Long): Room {
        val room = roomRepository.findByIdOrNull(roomId)
        if (room == null || room.environment?.owner?.id != ownerId) {
            log.warn("Recusa: cômodo id={} não encontrado ou não pertence ao usuário id={}", roomId, ownerId)
            throw NotFoundException("Cômodo não encontrado.")
        }
        return room
    }

    private fun findOwnedDeviceOrThrow(deviceId: Long, ownerId: Long): Device {
        val device = deviceRepository.findByIdOrNull(deviceId)
        if (device == null || device.owner?.id != ownerId) {
            log.warn("Recusa: dispositivo id={} não encontrado ou não pertence ao usuário id={}", deviceId, ownerId)
            throw NotFoundException("Dispositivo não encontrado.")
        }
        return device
    }

    private fun populateReportSummary(report: Report) {
        report.generatedAt = Instant.now()
    }
}