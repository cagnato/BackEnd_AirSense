package br.pucpr.airsense.environments

import br.pucpr.airsense.common.buildSort
import br.pucpr.airsense.exception.NotFoundException
import br.pucpr.airsense.reports.ReportRepository // <-- NOVO (BK.21)
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional // <-- NOVO (BK.21)

@Service
class EnvironmentService(
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository,
    private val reportRepository: ReportRepository // <-- NOVO (BK.21)
) {
    private val log = LoggerFactory.getLogger(EnvironmentService::class.java)

    companion object {
        private val ALLOWED_SORT_FIELDS = setOf("id", "name", "city")
    }

    fun create(req: EnvironmentRequest): EnvironmentResponse {
        val env = Environment(
            name = req.name,
            description = req.description,
            city = req.city
        )
        val saved = environmentRepository.save(env)
        log.info("Ambiente Macro criado: id={}, name={}", saved.id, saved.name)
        return EnvironmentResponse(saved)
    }

    fun search(name: String?, city: String?, sortBy: String, sortDir: String): List<EnvironmentResponse> {
        val sort = buildSort(sortBy, sortDir, ALLOWED_SORT_FIELDS)
        return environmentRepository.findWithFilters(name, city, sort)
            .map { EnvironmentResponse(it) }
    }

    fun findById(id: Long): EnvironmentResponse {
        val env = environmentRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Ambiente com id=$id não encontrado")
        return EnvironmentResponse(env)
    }

    fun update(id: Long, req: EnvironmentRequest): EnvironmentResponse {
        val env = environmentRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Ambiente com id=$id não encontrado")

        env.name = req.name
        env.description = req.description
        env.city = req.city

        val saved = environmentRepository.save(env)
        log.info("Ambiente atualizado: id={}", saved.id)
        return EnvironmentResponse(saved)
    }

    @Transactional // <-- NOVO (BK.21)
    fun delete(id: Long) {
        val env = environmentRepository.findByIdOrNull(id)
            ?: throw NotFoundException("Ambiente com id=$id não encontrado")

        // <-- NOVO (BK.21 - D33: deixa vínculo nulo nos relatórios antes de apagar cômodos e ambiente)
        env.rooms.forEach { room ->
            room.id?.let { reportRepository.unlinkRoom(it) }
        }
        reportRepository.unlinkEnvironment(id)

        environmentRepository.delete(env)
        log.info("Ambiente removido: id={}", id)
    }

    fun addRoom(environmentId: Long, req: RoomRequest): EnvironmentResponse {
        val env = environmentRepository.findByIdOrNull(environmentId)
            ?: throw NotFoundException("Ambiente com id=$environmentId não encontrado")

        val room = Room(
            name = req.name,
            areaM2 = req.areaM2,
            environment = env
        )
        env.rooms.add(room)
        val saved = environmentRepository.save(env)
        log.info("Cômodo '{}' adicionado ao ambiente id={}", req.name, environmentId)
        return EnvironmentResponse(saved)
    }

    @Transactional // <-- NOVO (BK.21)
    fun removeRoom(environmentId: Long, roomId: Long): EnvironmentResponse {
        val env = environmentRepository.findByIdOrNull(environmentId)
            ?: throw NotFoundException("Ambiente com id=$environmentId não encontrado")

        val exists = env.rooms.any { it.id == roomId }
        if (!exists) {
            throw NotFoundException("Cômodo id=$roomId não encontrado neste ambiente")
        }

        reportRepository.unlinkRoom(roomId) // <-- NOVO (BK.21 - D33: deixa vínculo nulo nos relatórios)
        env.rooms.removeIf { it.id == roomId }

        val saved = environmentRepository.save(env)
        log.info("Cômodo id={} removido do ambiente id={}", roomId, environmentId)
        return EnvironmentResponse(saved)
    }
}