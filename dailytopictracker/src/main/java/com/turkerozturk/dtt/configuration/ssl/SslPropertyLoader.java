package com.turkerozturk.dtt.configuration.ssl;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SslPropertyLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();

        boolean sslEnabled = Boolean.parseBoolean(env.getProperty("app.ssl.enabled", "true"));

        if (!sslEnabled) {
            return; // zaten HTTP üzerinden çalışacak
        }

        // JAR ile aynı klasördeki ./ssl/keystore.p12 yolunu kontrol et
        File sslFile = new File("ssl/keystore.p12");

        Map<String, Object> props = new HashMap<>();
        if (sslFile.exists()) {
            // Dışarıdan gelen sertifikayı kullan
            props.put("server.ssl.key-store", sslFile.getAbsolutePath());
            props.put("server.ssl.key-store-password", env.getProperty("server.ssl.key-store-password", "changeit"));
            props.put("server.ssl.key-store-type", env.getProperty("server.ssl.key-store-type", "PKCS12"));
            System.out.println("🔐 External SSL keystore loaded: " + sslFile.getAbsolutePath());
        } else {
            // Default (classpath içindeki self-signed)
            props.put("server.ssl.key-store", "classpath:default-keystore.p12");
            props.put("server.ssl.key-store-password", env.getProperty("server.ssl.key-store-password", "changeit"));
            props.put("server.ssl.key-store-type", "PKCS12");
            System.out.println("🔒 By default, self-signed SSL is used.");
        }

        env.getPropertySources().addFirst(new MapPropertySource("dynamic-ssl-config", props));
    }
}

