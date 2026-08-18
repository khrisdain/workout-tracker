package com.fitnesstracker.workouttracker.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * The {@link RestTemplate} the primary application uses to talk to the
 * nutrition microservice. Short timeouts matter here: a hung microservice must
 * not hold a request thread long enough to make PulseTrack itself feel broken.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate nutritionRestTemplate(RestTemplateBuilder builder, PulseTrackProperties properties) {
        PulseTrackProperties.NutritionService cfg = properties.getNutritionService();
        return builder
                .rootUri(cfg.getBaseUrl())
                .basicAuthentication(cfg.getUsername(), cfg.getPassword())
                .setConnectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
                // Let 4xx/5xx come back as exceptions the client layer translates
                // into a degraded-but-usable page rather than a stack trace.
                .errorHandler(new DefaultResponseErrorHandler())
                .build();
    }
}
