package br.pucpr.airsense.limits

import br.pucpr.airsense.exception.BadRequestException
import org.slf4j.LoggerFactory

/** Regras dos limites, sem acesso a banco (testadas em SafetyLimitsRulesTest). */
object SafetyLimitsRules {
    const val MIN_MAX_MESSAGE = "O valor mínimo deve ser menor que o máximo."

    private val log = LoggerFactory.getLogger(SafetyLimitsRules::class.java)

    /** Medidas cujo mínimo não é menor que o máximo (lista vazia = tudo certo). */
    fun invalidRanges(req: SafetyLimitsRequest): List<String> = listOfNotNull(
        "temp".takeIf { !isValid(req.tempMinSafe, req.tempMaxSafe) },
        "humidity".takeIf { !isValid(req.humidityMinSafe, req.humidityMaxSafe) },
        "airQuality".takeIf { !isValid(req.airQualityMinSafe, req.airQualityMaxSafe) },
        "co2".takeIf { !isValid(req.co2MinSafe, req.co2MaxSafe) }
    )

    /** US015 cenário 3: mínimo igual ou maior que o máximo é recusado com a mensagem do critério de aceite. */
    fun validate(req: SafetyLimitsRequest, owner: String = "") {
        val invalid = invalidRanges(req)
        if (invalid.isNotEmpty()) {
            log.warn("Limites recusados {}: mínimo não é menor que o máximo em {}", owner, invalid)
            throw BadRequestException(MIN_MAX_MESSAGE)
        }
    }

    /**
     * Novo valor dos limites de um dono depois de um pedido:
     * pedido nulo = sem limites próprios; senão valida e atualiza os atuais (ou cria).
     */
    fun merge(current: SafetyLimits?, req: SafetyLimitsRequest?, owner: String): SafetyLimits? {
        if (req == null) return null
        validate(req, owner)
        return (current ?: SafetyLimits()).also { it.apply(req) }
    }

    /**
     * Monta a visão dos limites de um nível: o próprio (ou nulo) e o que vale de fato.
     * Vale o mais específico definido: dispositivo -> cômodo -> ambiente -> padrão do sistema.
     */
    fun view(
        own: SafetyLimits?,
        device: SafetyLimits? = null,
        room: SafetyLimits? = null,
        environment: SafetyLimits? = null
    ): LimitsView {
        val (effective, source) = when {
            device != null -> SafetyLimitsResponse(device) to LimitsSource.DEVICE
            room != null -> SafetyLimitsResponse(room) to LimitsSource.ROOM
            environment != null -> SafetyLimitsResponse(environment) to LimitsSource.ENVIRONMENT
            else -> SafetyLimitsResponse.DEFAULT to LimitsSource.DEFAULT
        }
        return LimitsView(own?.let { SafetyLimitsResponse(it) }, effective, source)
    }

    private fun isValid(min: Double?, max: Double?): Boolean =
        min == null || max == null || min < max
}
