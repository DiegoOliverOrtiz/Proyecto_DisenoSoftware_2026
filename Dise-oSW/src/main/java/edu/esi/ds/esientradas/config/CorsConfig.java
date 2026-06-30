package edu.esi.ds.esientradas.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:${app.cors.allowed-origin:http://localhost:4200,http://127.0.0.1:4200}}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Origin", "X-Requested-With", "X-Queue-Access", "X-Queue-Client")
                .allowCredentials(true);
    }

    private String[] origins() {
        String[] configuredOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .filter(origin -> !"*".equals(origin))
                .filter(origin -> !"null".equalsIgnoreCase(origin))
                .toArray(String[]::new);
        if (configuredOrigins.length == 0) {
            return new String[] {"http://localhost:4200", "http://127.0.0.1:4200"};
        }
        return configuredOrigins;
    }
}
