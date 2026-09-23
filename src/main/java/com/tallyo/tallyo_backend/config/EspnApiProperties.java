package com.tallyo.tallyo_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "espn.api")
public class EspnApiProperties {

    private String baseUrl;
    // sports.core.api.espn.com: resources the site API doesn't expose (e.g. depth charts).
    private String coreBaseUrl;
    private Scoreboard scoreboard = new Scoreboard();

    @Data
    public static class Scoreboard {
        private int limit;
    }
}
