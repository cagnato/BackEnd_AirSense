package br.pucpr.airsense.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val jwtTokenFilter: JwtTokenFilter) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(security: HttpSecurity): SecurityFilterChain =
        security
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .headers { headers -> headers.frameOptions { it.disable() } } // Permite o H2 Console
            .authorizeHttpRequests { requests ->
                requests
                    // Rotas utilitárias (H2 e Swagger)
                    .requestMatchers("/h2-console/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                    // Rotas públicas de autenticação e registo
                    .requestMatchers(HttpMethod.POST, "/users", "/users/login").permitAll()
                    // Consultas públicas (GET)
                    .requestMatchers(HttpMethod.GET, "/users/**", "/environments/**", "/devices/**").permitAll()
                    // Ações destrutivas (DELETE/PUT) e restantes rotas exigem Token JWT válido
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtTokenFilter, BasicAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            addAllowedHeader("*")
            addAllowedOrigin("*")
            addAllowedMethod("*")
        }
        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
        return CorsFilter(source)
    }
}