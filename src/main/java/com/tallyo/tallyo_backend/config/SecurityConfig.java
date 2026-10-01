package com.tallyo.tallyo_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;

/**
 * Two ways in, each with its own filter chain:
 * <ol>
 *   <li>{@code /mcp/**}: OAuth bearer tokens from Keycloak (for MCP clients such as claude.ai),
 *       plus the public protected-resource metadata MCP clients use to find Keycloak.</li>
 *   <li>Everything else: the shared {@code x-api-key}, as before (frontend proxy, pipelines).</li>
 * </ol>
 * The chains don't overlap: an API key doesn't open {@code /mcp/**}, and a token doesn't open
 * the API-key routes.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(OAuthProperties.class)
public class SecurityConfig {

    static final String METADATA_PATH = "/.well-known/oauth-protected-resource";

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final OAuthProperties oauth;

    public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter, OAuthProperties oauth) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.oauth = oauth;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain mcpFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/mcp/**", METADATA_PATH, METADATA_PATH + "/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(METADATA_PATH, METADATA_PATH + "/**").permitAll()
                        .anyRequest().access(AuthorizationManagers.allOf(
                                hasAuthority("SCOPE_" + oauth.scope()),
                                hasAuthority("GROUP_" + oauth.requiredGroup()))))
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        // RFC 9728 metadata; 401s point to it via WWW-Authenticate resource_metadata.
                        // Spring prefills bearer method "header" and marks tokens as mTLS-bound; ours
                        // are plain bearer tokens, so that flag must be false or clients may refuse.
                        .protectedResourceMetadata(meta -> meta.protectedResourceMetadataCustomizer(m -> m
                                .resource(oauth.resource())
                                .authorizationServer(oauth.issuer())
                                .scope(oauth.scope())
                                .tlsClientCertificateBoundAccessTokens(false)
                                .resourceName("tallyo"))));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * ApiKeyAuthFilter is a {@code @Component}, so Spring Boot would also register it as a global
     * servlet filter, running on every request (including {@code /mcp/**}) before either chain.
     * It belongs only in the API-key chain.
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Keycloak's keys are fetched on first use rather than at startup, so the API still boots
     * (and serves the API-key routes) if auth.tallyo.us is unreachable.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(oauth.issuer()).build();
            decoder.setJwtValidator(tokenValidator(oauth));
            return decoder;
        });
    }

    /** Signature checks happen in the decoder; this adds expiry, issuer, and audience. */
    static OAuth2TokenValidator<Jwt> tokenValidator(OAuthProperties oauth) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(oauth.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(oauth.resource())));
    }

    /** Authorities: {@code SCOPE_<scope>} from {@code scope}, {@code GROUP_<group>} from {@code groups}. */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            List<String> groups = jwt.getClaimAsStringList("groups");
            if (groups != null) {
                groups.forEach(group -> authorities.add(new SimpleGrantedAuthority("GROUP_" + group)));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://tallyo.us"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
