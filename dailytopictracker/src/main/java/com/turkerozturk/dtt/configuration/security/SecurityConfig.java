/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.dtt.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // application.properties'e hangi enum degerini yazdiyak o sayfaya yonlenmesi icin.
    @Value("${app.login.redirectTarget:HOME}")
    private String redirectTargetRaw; // dogrudan RedirectTarget yapmadik cunku yanlis yazim varsa hata vermemesi icin fromString metodumuzla kontrol etmek istiyoruz..

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        RedirectTarget redirectTarget = RedirectTarget.fromString(redirectTargetRaw);

        return http
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl(redirectTarget.getUrl(), true)  // <<--- login olunca gidecegi sayfayi artik properties dosyasindan aliyor.
                        .permitAll()
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health", "/img/**", "/static/**", "/webjars/**", "/static/css/**", "/api/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .build();
    }
}
