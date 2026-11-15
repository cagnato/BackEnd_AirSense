package br.pucpr.airsense.common

import br.pucpr.airsense.exception.BadRequestException
import org.springframework.data.domain.Sort

fun buildSort(
    sortBy: String,
    sortDir: String,
    allowedFields: Set<String>,
    defaultField: String = allowedFields.first()
): Sort {
    val field = sortBy.ifBlank { defaultField }
    if (field !in allowedFields) {
        throw BadRequestException(
            "Campo de ordenação inválido: '$field'. Campos permitidos: ${allowedFields.joinToString(", ")}"
        )
    }

    val direction = when (sortDir.lowercase()) {
        "asc" -> Sort.Direction.ASC
        "desc" -> Sort.Direction.DESC
        else -> throw BadRequestException(
            "Direção de ordenação inválida: '$sortDir'. Use 'asc' ou 'desc'."
        )
    }

    return Sort.by(direction, field)
}