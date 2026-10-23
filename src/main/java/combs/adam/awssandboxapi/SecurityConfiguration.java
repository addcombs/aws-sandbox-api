package combs.adam.awssandboxapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.auth.issuer-uri}") String issuerUri,
            OAuth2TokenValidator<Jwt> cognitoAccessTokenValidator
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(issuerUri + "/.well-known/jwks.json")
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                cognitoAccessTokenValidator));
        return decoder;
    }

    @Bean
    OAuth2TokenValidator<Jwt> cognitoAccessTokenValidator(
            @Value("${app.auth.client-id}") String clientId
    ) {
        return jwt -> {
            if (!"access".equals(jwt.getClaimAsString("token_use"))) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "A Cognito access token is required", null));
            }
            if (!clientId.equals(jwt.getClaimAsString("client_id"))) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The token was issued to another app client", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }
}
