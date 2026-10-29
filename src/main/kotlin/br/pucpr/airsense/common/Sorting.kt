package br.pucpr.airsense.common

import br.pucpr.airsense.exception.BadRequestException
import org.springframework.data.domain.Sort

/**
 * Valida os parâmetros de ordenação e monta o Sort.
 * Campo fora de [allowedFields] ou direção diferente de asc/desc gera 400.
 * O id sempre desempata.
 */
fun buildSort(sortBy: String, sortDir: String, allowedFields: Set<String>): Sort {
    if (sortBy !in allowedFields) {
        throw BadRequestException("Campo de ordenação inválido: $sortBy. Use: $allowedFields")
    }
    val direction = when (sortDir.lowercase()) {
        "asc" -> Sort.Direction.ASC
        "desc" -> Sort.Direction.DESC
        else -> throw BadRequestException("Direção de ordenação inválida: $sortDir. Use: asc ou desc")
    }
    val sort = Sort.by(direction, sortBy)
    return if (sortBy == "id") sort else sort.and(Sort.by(Sort.Direction.ASC, "id"))
}
