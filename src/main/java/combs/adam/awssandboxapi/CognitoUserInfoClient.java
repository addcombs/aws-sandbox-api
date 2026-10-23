package combs.adam.awssandboxapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class CognitoUserInfoClient {

    private final RestClient restClient;
    private final String userInfoUri;

    public CognitoUserInfoClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.auth.user-info-uri}") String userInfoUri
    ) {
        this.restClient = restClientBuilder.build();
        this.userInfoUri = userInfoUri;
    }

    public CognitoUserInfo getUserInfo(String accessToken) {
        try {
            return restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(CognitoUserInfo.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Unable to retrieve the authenticated user's profile from Cognito.",
                    exception);
        }
    }
}
