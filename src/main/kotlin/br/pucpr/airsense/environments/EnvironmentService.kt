package br.pucpr.airsense.environments

import br.pucpr.airsense.exception.BadRequestException
import br.pucpr.airsense.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class EnvironmentService(
    private val environmentRepository: EnvironmentRepository,
    private val roomRepository: RoomRepository
) {
    private val log = LoggerFactory.getLogger(EnvironmentService::class.java)

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
        val allowedSortFields = setOf("id", "name", "city")
        if (sortBy !in allowedSortFields) {
            throw BadRequestException("Campo de ordenação inválido: $sortBy. Use: $allowedSortFields")
        }

        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val sort = Sort.by(direction, sortBy)

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

    fun delete(id: Long) {
        if (!environmentRepository.existsById(id)) {
            throw NotFoundException("Ambiente com id=$id não encontrado")
        }
        environmentRepository.deleteById(id)
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

    fun removeRoom(environmentId: Long, roomId: Long): EnvironmentResponse {
        val env = environmentRepository.findByIdOrNull(environmentId)
            ?: throw NotFoundException("Ambiente com id=$environmentId não encontrado")

        val removed = env.rooms.removeIf { it.id == roomId }
        if (!removed) {
            throw NotFoundException("Cômodo id=$roomId não encontrado neste ambiente")
        }

        val saved = environmentRepository.save(env)
        log.info("Cômodo id={} removido do ambiente id={}", roomId, environmentId)
        return EnvironmentResponse(saved)
    }
}