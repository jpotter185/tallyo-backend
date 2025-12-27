package com.tallyo.tallyo_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "espn.api")
public class EspnApiProperties {

    private String baseUrl;
    private Scoreboard scoreboard = new Scoreboard();

    @Data
    public static class Scoreboard {
        private int limit;
    }
}
