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
package com.turkerozturk.dtt.configuration.externalresources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${static.caching.duration:86400}")
    private int staticCachingDuration; // 60 * 60 * 24 * 1 = 86400 = one day

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // "topicimages/" URL'sini "topicimages/" klasorune bagla
        String externalPath = Paths.get("topicimages").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/topicimages/**")
                .addResourceLocations(externalPath)
                .setCachePeriod(staticCachingDuration);

        // https://github.com/spring-projects/spring-framework/issues/29322
        // https://www.webjars.org/documentation#springmvc
        // https://jamesward.com/2012/04/30/webjars-in-spring-mvc/
        // registry.addResourceHandler("/webjars/**")
        // .addResourceLocations("classpath:/META-INF/resources/webjars/") // bu ise yaramiyor. Aasagidaki problemsiz:
        //  Note: In a Servlet 3 container the registry.addResourceHandler line can be simplified to:
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("/webjars/")
                .setCachePeriod(staticCachingDuration)
                .resourceChain(true);
              //  .addResolver(new WebJarsResourceResolver()); // soldakine gerek yok, deprecated, kirmizi cizer zaten.
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(staticCachingDuration);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(staticCachingDuration);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(staticCachingDuration);

    }
}

