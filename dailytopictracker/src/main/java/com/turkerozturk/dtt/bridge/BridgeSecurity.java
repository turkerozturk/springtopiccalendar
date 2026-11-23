package com.turkerozturk.dtt.bridge;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class BridgeSecurity {
    @Bean
    @Order(1)
    SecurityFilterChain bridgeFilterChain(HttpSecurity http,
                                          @Value("${dtt.bridge.token:}") String token) throws Exception {
        return http.securityMatcher("/api/bridge/v1/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new OncePerRequestFilter() {
                    private final boolean configured = token.matches("[A-Za-z0-9_-]{32,256}");
                    private final byte[] authorization = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);

                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                    FilterChain chain) throws ServletException, IOException {
                        response.setHeader("Cache-Control", "no-store");
                        if (!configured) {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                        String supplied = request.getHeader("Authorization");
                        if (supplied == null || !MessageDigest.isEqual(authorization,
                                supplied.getBytes(StandardCharsets.UTF_8))) {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        chain.doFilter(request, response);
                    }
                }, AnonymousAuthenticationFilter.class)
                .build();
    }
}
