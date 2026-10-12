package com.mailscale.config;

import com.mailscale.auth.AuthenticationFilter;
import com.mailscale.auth.UnauthorizedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;
    private final UnauthorizedHandler unauthorizedHandler;

    public SecurityConfig(AuthenticationFilter authenticationFilter,
                            UnauthorizedHandler unauthorizedHandler) {
        this.authenticationFilter = authenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No anonymous token: with anonymous disabled, an
                // unauthenticated request on a protected route fails
                // with AccessDenied that our entry point maps to 401
                // instead of Spring's default 403.
                .anonymous(anonymous -> anonymous.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, ex2) ->
                                unauthorizedHandler.handle(
                                        request,
                                        response,
                                        "Authentication required")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health",
                                "/actuator/info").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui/**",
                                "/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/emails")
                            .hasAnyAuthority("ROLE_OWNER", "ROLE_ADMIN",
                                    "ROLE_MARKETER", "ROLE_APIKEY")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(authenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
