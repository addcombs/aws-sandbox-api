package combs.adam.awssandboxapi;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.auth.issuer-uri=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "app.auth.client-id=test-client",
        "app.auth.user-info-uri=https://test.auth.us-east-1.amazoncognito.com/oauth2/userInfo"
})
@AutoConfigureMockMvc
class AwsSandboxApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuth2TokenValidator<Jwt> cognitoAccessTokenValidator;

    @MockitoBean
    private CognitoUserInfoClient cognitoUserInfoClient;

    @Test
    void exposesHealthEndpointToConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:4200"));
    }

    @Test
    void supportsUserApiPreflightFromConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:4200"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().string(
                        "Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("authorization")));
    }

    @Test
    void doesNotAllowCorsForAnUnconfiguredOrigin() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void requiresAuthenticationForProfilesAndDoesNotExposeTheUserDirectory() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptsOnlyAccessTokensForTheConfiguredCognitoClient() {
        Jwt validToken = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("cognito-sub-123")
                .claim("client_id", "test-client")
                .claim("token_use", "access")
                .build();
        Jwt idToken = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("cognito-sub-123")
                .claim("client_id", "test-client")
                .claim("token_use", "id")
                .build();
        Jwt otherClientToken = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("cognito-sub-123")
                .claim("client_id", "another-client")
                .claim("token_use", "access")
                .build();

        assertFalse(cognitoAccessTokenValidator.validate(validToken).hasErrors());
        assertTrue(cognitoAccessTokenValidator.validate(idToken).hasErrors());
        assertTrue(cognitoAccessTokenValidator.validate(otherClientToken).hasErrors());
    }

    @Test
    void mapsCognitoUserInfoSnakeCaseClaims() throws Exception {
        CognitoUserInfo profile = objectMapper.readValue("""
                {
                  "sub": "cognito-sub-123",
                  "email": "ada@example.com",
                  "email_verified": true,
                  "given_name": "Ada",
                  "family_name": "Lovelace"
                }
                """, CognitoUserInfo.class);

        assertEquals("cognito-sub-123", profile.sub());
        assertEquals("Ada", profile.given_name());
        assertEquals("Lovelace", profile.family_name());
        assertTrue(profile.email_verified());
    }

    @Test
    void synchronizesTheAuthenticatedUsersCognitoProfile() throws Exception {
        userRepository.deleteAll();
        when(cognitoUserInfoClient.getUserInfo("token"))
                .thenReturn(new CognitoUserInfo(
                        "cognito-sub-123",
                        "ada@example.com",
                        true,
                        "Ada",
                        "Lovelace"))
                .thenReturn(new CognitoUserInfo(
                        "cognito-sub-123",
                        "ada.new@example.com",
                        true,
                        "Augusta",
                        "King"));

        mockMvc.perform(get("/api/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cognito-sub-123")
                                        .claim("client_id", "test-client")
                                        .claim("token_use", "access"))
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.emailAddress").value("ada@example.com"))
                .andExpect(jsonPath("$.cognitoSub").doesNotExist());

        mockMvc.perform(get("/api/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cognito-sub-123")
                                        .claim("client_id", "test-client")
                                        .claim("token_use", "access"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Augusta"))
                .andExpect(jsonPath("$.emailAddress").value("ada.new@example.com"));

        assertEquals(1, userRepository.count());
        User savedUser = userRepository.findAll().getFirst();
        assertEquals("cognito-sub-123", savedUser.getCognitoSub());
        assertEquals("ada.new@example.com", savedUser.getEmailAddress());
    }

    @Test
    void doesNotPersistAnUnverifiedEmailAddress() throws Exception {
        userRepository.deleteAll();
        when(cognitoUserInfoClient.getUserInfo("token"))
                .thenReturn(new CognitoUserInfo(
                        "cognito-sub-123",
                        "ada@example.com",
                        false,
                        "Ada",
                        "Lovelace"));

        mockMvc.perform(get("/api/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("cognito-sub-123")
                                        .claim("client_id", "test-client")
                                        .claim("token_use", "access"))))
                .andExpect(status().isForbidden());

        assertEquals(0, userRepository.count());
    }
}
