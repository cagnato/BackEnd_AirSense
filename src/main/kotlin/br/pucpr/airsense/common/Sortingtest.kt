package br.pucpr.airsense.common

import br.pucpr.airsense.exception.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort

class SortingTest {

    private val allowed = setOf("id", "name", "city")

    @Test
    fun `ordena pelo campo pedido e desempata pelo id`() {
        val sort = buildSort("name", "desc", allowed)

        val orders = sort.toList()
        assertEquals(listOf("name", "id"), orders.map { it.property })
        assertEquals(Sort.Direction.DESC, orders[0].direction)
        assertEquals(Sort.Direction.ASC, orders[1].direction)
    }

    @Test
    fun `ordenar por id nao duplica o desempate`() {
        val sort = buildSort("id", "asc", allowed)

        assertEquals(listOf("id"), sort.toList().map { it.property })
    }

    @Test
    fun `direcao e aceita sem diferenciar maiusculas`() {
        val sort = buildSort("city", "ASC", allowed)

        assertEquals(Sort.Direction.ASC, sort.toList().first().direction)
    }

    @Test
    fun `campo fora da lista gera BadRequest`() {
        assertThrows(BadRequestException::class.java) {
            buildSort("password", "asc", allowed)
        }
    }

    @Test
    fun `direcao invalida gera BadRequest`() {
        assertThrows(BadRequestException::class.java) {
            buildSort("name", "sideways", allowed)
        }
    }
}