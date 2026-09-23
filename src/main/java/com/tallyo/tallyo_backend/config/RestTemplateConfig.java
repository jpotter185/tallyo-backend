package com.tallyo.tallyo_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
public class RestTemplateConfig {
    // ESPN's edge (Akamai) 403s the default Java HTTP client User-Agent (and a
    // spoofed browser one, and Wget's, and Postman's) but consistently lets
    // curl's own UA through, verified directly against the live endpoint.
    private static final String ESPN_USER_AGENT = "curl/8.7.1";

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().set("User-Agent", ESPN_USER_AGENT);
            return execution.execute(request, body);
        }));
        return restTemplate;
    }
}
