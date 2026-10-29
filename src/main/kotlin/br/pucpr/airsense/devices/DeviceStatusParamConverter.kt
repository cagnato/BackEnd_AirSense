package br.pucpr.airsense.devices

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

// Permite ?status=ACTIVE ou ?status=1 (código do app) nos query params.
@Component
class DeviceStatusParamConverter : Converter<String, DeviceStatus> {
    override fun convert(source: String): DeviceStatus = DeviceStatus.from(source)
}
