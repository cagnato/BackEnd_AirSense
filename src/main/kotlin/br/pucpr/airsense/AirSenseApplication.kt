package br.pucpr.airsense

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AirSenseApplication

fun main(args: Array<String>) {
	runApplication<AirSenseApplication>(*args)
}
