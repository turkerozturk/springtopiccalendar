package com.turkerozturk.dtt.configuration.ssl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SslConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value("${app.ssl.enabled:true}") // default true
    private boolean sslEnabled;

    @Value("${server.port:8443}")
    private int sslPort;

    @Value("${server.http.port:8080}")
    private int httpPort;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        if (!sslEnabled) {
            // SSL kapalı → HTTP'den başlat
            factory.setPort(httpPort);
            factory.setSsl(null); // SSL config temizle
        } else {
            // SSL açık → mevcut server.ssl.* ayarları geçerli
            factory.setPort(sslPort);
        }
    }
}

