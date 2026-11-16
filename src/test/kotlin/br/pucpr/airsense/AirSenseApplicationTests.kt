package br.pucpr.airsense

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// Banco em memória: o teste não mexe no ./data do servidor de desenvolvimento.
@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:airsense-test;DB_CLOSE_DELAY=-1"])
class AirSenseApplicationTests {

	@Test
	fun contextLoads() {
	}

}
