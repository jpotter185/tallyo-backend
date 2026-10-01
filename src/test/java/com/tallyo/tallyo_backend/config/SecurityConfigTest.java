package com.tallyo.tallyo_backend.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.tallyo.tallyo_backend.controller.LeagueController;
import com.tallyo.tallyo_backend.controller.McpWhoamiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Both security chains end to end. Tokens are real signed JWTs checked by the production
 * validators (expiry, issuer, audience) and authorization rules (scope + group); only the
 * signing key is a test key instead of Keycloak's.
 */
@WebMvcTest(controllers = {LeagueController.class, McpWhoamiController.class},
        properties = "api.key=test-api-key")
@Import({SecurityConfig.class, SecurityConfigTest.TestKeys.class})
class SecurityConfigTest {

    private static final String ISSUER = "https://auth.tallyo.us/realms/tallyo";
    private static final String AUDIENCE = "https://api.tallyo.us/mcp";
    private static final KeyPair KEYS = rsaKeyPair();
    private static final KeyPair OTHER_KEYS = rsaKeyPair();

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class TestKeys {
        /** Production validators, test public key. */
        @Bean
        @Primary
        JwtDecoder testJwtDecoder(OAuthProperties oauth) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEYS.getPublic()).build();
            decoder.setJwtValidator(SecurityConfig.tokenValidator(oauth));
            return decoder;
        }
    }

    // ---- API-key routes: unchanged behavior ----

    @Test
    void apiKeyOpensApiRoutes() throws Exception {
        mvc.perform(get("/api/v1/leagues").header("x-api-key", "test-api-key")).andExpect(status().isOk());
    }

    @Test
    void apiRoutesRejectMissingOrWrongKey() throws Exception {
        mvc.perform(get("/api/v1/leagues")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/leagues").header("x-api-key", "nope")).andExpect(status().isForbidden());
    }

    @Test
    void tokenDoesNotOpenApiRoutes() throws Exception {
        mvc.perform(get("/api/v1/leagues").header("Authorization", "Bearer " + token(c -> {})))
                .andExpect(status().isForbidden());
    }

    // ---- MCP routes: OAuth ----

    @Test
    void validTokenInGroupReachesMcp() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer " + token(c -> {})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-123"))
                .andExpect(jsonPath("$.username").value("jack"))
                .andExpect(jsonPath("$.groups[0]").value("tallyo-backend"))
                .andExpect(jsonPath("$.scopes").value(org.hamcrest.Matchers.hasItem("tallyo:read")));
    }

    @Test
    void noTokenGets401PointingToMetadata() throws Exception {
        mvc.perform(get("/mcp/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("resource_metadata=")))
                .andExpect(header().string("WWW-Authenticate", containsString("/.well-known/oauth-protected-resource")));
    }

    @Test
    void apiKeyDoesNotOpenMcp() throws Exception {
        mvc.perform(get("/mcp/whoami").header("x-api-key", "test-api-key")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongAudienceIsRejected() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.audience(List.of("https://weather.tallyo.us")))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.issuer("https://evil.example/realms/tallyo"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.issuedAt(past.minusSeconds(900)).expiresAt(past))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedByAnotherKeyIsRejected() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer " + token(OTHER_KEYS, c -> {})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userOutsideGroupIsForbidden() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.claims(claims -> claims.remove("groups")))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.claim("groups", List.of("someone-else")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithoutScopeIsForbidden() throws Exception {
        mvc.perform(get("/mcp/whoami").header("Authorization", "Bearer "
                        + token(c -> c.claim("scope", "openid profile"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void metadataIsPublic() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value(AUDIENCE))
                .andExpect(jsonPath("$.authorization_servers[0]").value(ISSUER))
                .andExpect(jsonPath("$.scopes_supported[0]").value("tallyo:read"))
                .andExpect(jsonPath("$.bearer_methods_supported[0]").value("header"));
    }

    // ---- helpers ----

    /** A token like Keycloak issues for a member of tallyo-backend; {@code edit} changes it. */
    private static String token(Consumer<JwtClaimsSet.Builder> edit) {
        return token(KEYS, edit);
    }

    private static String token(KeyPair keys, Consumer<JwtClaimsSet.Builder> edit) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("user-123")
                .audience(List.of(AUDIENCE, "account"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("scope", "openid profile email tallyo:read")
                .claim("groups", List.of("tallyo-backend"))
                .claim("preferred_username", "jack");
        edit.accept(claims);
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
                .privateKey((RSAPrivateKey) keys.getPrivate()).keyID("test").build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test").build(), claims.build())).getTokenValue();
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
