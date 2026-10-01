package com.tallyo.tallyo_backend.controller;

import com.tallyo.tallyo_backend.dto.WhoamiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Echoes the caller's OAuth token: an end-to-end check that sign-in through Keycloak reaches
 * the MCP routes. Security (scope + group) is enforced in SecurityConfig's MCP chain.
 */
@RestController
@RequestMapping("/mcp")
public class McpWhoamiController {

    @GetMapping("/whoami")
    public WhoamiResponse whoami(@AuthenticationPrincipal Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        String scope = jwt.getClaimAsString("scope");
        return WhoamiResponse.builder()
                .subject(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .groups(Objects.requireNonNullElse(groups, List.of()))
                .scopes(scope == null ? List.of() : Arrays.asList(scope.split(" ")))
                .build();
    }
}
