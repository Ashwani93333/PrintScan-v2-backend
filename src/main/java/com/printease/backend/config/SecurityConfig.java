package com.printease.backend.config;

import com.printease.backend.security.AuthEntryPoint;
import com.printease.backend.security.JwtAuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthEntryPoint authEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * Expose the CSRF token repository as a named bean so it can be injected
     * into AuthController for the /auth/csrf priming endpoint.
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * Eagerly writes the XSRF-TOKEN cookie on every response.
     * Spring Security 6 uses a deferred/lazy CSRF token by default — the cookie
     * is only set when something explicitly reads the token. This filter ensures
     * the cookie is always present so the frontend can read it on any page load.
     */
    private OncePerRequestFilter csrfCookieFilter(CsrfTokenRepository repo) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrf = repo.loadToken(request);
                if (csrf == null) {
                    csrf = repo.generateToken(request);
                    repo.saveToken(csrf, request, response);
                } else {
                    repo.saveToken(csrf, request, response);
                }
                // Trigger attribute-level deferred access too
                request.setAttribute(CsrfToken.class.getName(), csrf);
                
                // Expose token via header for cross-origin frontend
                response.setHeader(csrf.getHeaderName(), csrf.getToken());
                
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRepository csrfRepo = csrfTokenRepository();

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> {
                    // Use plain handler (not XorCsrf) so the raw cookie value == the header value.
                    // XorCsrfTokenRequestAttributeHandler (Spring Security 6 default) masks the token,
                    // causing a mismatch when the frontend reads XSRF-TOKEN cookie and sends it as-is.
                    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                    csrf
                            .csrfTokenRepository(csrfRepo)
                            .csrfTokenRequestHandler(requestHandler)
                            .ignoringRequestMatchers("/api/auth/login", "/api/auth/logout", "/api/public/**");
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Swagger / OpenAPI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/v3/api-docs").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Super Admin endpoints
                        .requestMatchers("/api/super/**").hasRole("SUPER_ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Eagerly write XSRF-TOKEN cookie on every response
                .addFilterBefore(csrfCookieFilter(csrfRepo), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
