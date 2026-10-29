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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenFilter: JwtTokenFilter,
    private val restSecurityHandlers: RestSecurityHandlers
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // antMatcher explícito: o console do H2 registra um segundo servlet, e o Spring Security
    // não aceita matchers de String ambíguos nesse cenário.
    @Bean
    fun filterChain(security: HttpSecurity): SecurityFilterChain =
        security
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .headers { headers -> headers.frameOptions { it.disable() } } // Permite o H2 Console
            .exceptionHandling {
                it.authenticationEntryPoint(restSecurityHandlers)
                it.accessDeniedHandler(restSecurityHandlers)
            }
            .authorizeHttpRequests { requests ->
                requests
                    // Rotas utilitárias (H2, Swagger e página de erro)
                    .requestMatchers(
                        antMatcher("/h2-console/**"),
                        antMatcher("/v3/api-docs/**"),
                        antMatcher("/swagger-ui/**"),
                        antMatcher("/swagger-ui.html"),
                        antMatcher("/error")
                    ).permitAll()
                    // Rotas públicas: cadastro e login
                    .requestMatchers(
                        antMatcher(HttpMethod.POST, "/users"),
                        antMatcher(HttpMethod.POST, "/users/login")
                    ).permitAll()
                    // Todo o resto exige token JWT válido
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
