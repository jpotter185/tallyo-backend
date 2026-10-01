package com.tallyo.tallyo_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth settings for the MCP routes ({@code /mcp/**}). Tokens come from Keycloak
 * (repo {@code tallyo-auth}, https://auth.tallyo.us) and must carry this API's audience,
 * its scope, and membership in its group.
 *
 * @param issuer        Keycloak realm URL; tokens' {@code iss} must match exactly
 * @param resource      this API's identifier; must appear in tokens' {@code aud} and is the
 *                      {@code resource} in the protected-resource metadata
 * @param scope         scope required on every MCP request
 * @param requiredGroup Keycloak group whose members may use the MCP routes ({@code groups} claim)
 */
@ConfigurationProperties("tallyo.oauth")
public record OAuthProperties(String issuer, String resource, String scope, String requiredGroup) {
}
