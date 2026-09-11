package com.phabdev.starter.auth;

import com.phabdev.starter.user.UserRepository;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(
            HttpSecurity http, JwtService jwt, UserRepository users, AuthRequestFilter authRequests)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/auth/**",
                                                "/api/billing/webhook",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (request, response, ex) ->
                                                        error(
                                                                response,
                                                                401,
                                                                "Authentication required."))
                                        .accessDeniedHandler(
                                                (request, response, ex) ->
                                                        error(response, 403, "Access denied.")))
                .addFilterBefore(authRequests, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        new OncePerRequestFilter() {
                            @Override
                            protected void doFilterInternal(
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
                                    throws ServletException, IOException {
                                String header = request.getHeader("Authorization");
                                if (header != null && header.startsWith("Bearer ")) {
                                    try {
                                        var token = jwt.decode(header.substring(7));
                                        var account =
                                                users.byId(UUID.fromString(token.getSubject()));
                                        if (account.isPresent()
                                                && token.getClaim("ver") instanceof Number version
                                                && version.intValue()
                                                        == account.get().authVersion()) {
                                            var user = account.get();
                                            var authentication =
                                                    new UsernamePasswordAuthenticationToken(
                                                            user,
                                                            null,
                                                            List.of(
                                                                    new SimpleGrantedAuthority(
                                                                            "ROLE_"
                                                                                    + user.role()
                                                                                            .name())));
                                            SecurityContextHolder.getContext()
                                                    .setAuthentication(authentication);
                                        }
                                    } catch (JwtException | IllegalArgumentException ex) {
                                        SecurityContextHolder.clearContext();
                                    }
                                }
                                chain.doFilter(request, response);
                            }
                        },
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static void error(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter()
                .write(
                        "{\"status\":"
                                + status
                                + ",\"title\":\"Request rejected\",\"detail\":\""
                                + detail
                                + "\"}");
    }
}
