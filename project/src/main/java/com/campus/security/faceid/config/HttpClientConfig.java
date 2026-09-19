package com.campus.security.faceid.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

/**
 * Configuration for HTTP communication with external services
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // The Python AI service is a separate free-tier Render instance that spins down when
        // idle - a cold start plus loading the YuNet/ArcFace ONNX models can take well over the
        // previous 30s/60s limits, which was surfacing as a misleading "AI service unavailable"
        // error even though the service was fine, just asleep. Matches the tolerance already
        // used for the Android app's own calls to this backend for the same reason.
        return builder
                .setConnectTimeout(Duration.ofSeconds(90))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
    }
}
