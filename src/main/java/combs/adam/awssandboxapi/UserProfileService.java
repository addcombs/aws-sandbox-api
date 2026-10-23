package combs.adam.awssandboxapi;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final CognitoUserInfoClient cognitoUserInfoClient;

    public UserProfileService(
            UserRepository userRepository,
            CognitoUserInfoClient cognitoUserInfoClient
    ) {
        this.userRepository = userRepository;
        this.cognitoUserInfoClient = cognitoUserInfoClient;
    }

    public UserProfileResponse synchronize(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The token has no user subject.");
        }

        CognitoUserInfo profile = cognitoUserInfoClient.getUserInfo(jwt.getTokenValue());
        if (profile == null || !subject.equals(profile.sub())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cognito returned a profile that does not match the authenticated user.");
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cognito did not return an email address for the authenticated user.");
        }
        if (!Boolean.TRUE.equals(profile.email_verified())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Verify the email address before synchronizing the user profile.");
        }

        User user = userRepository.findByCognitoSub(subject).orElseGet(User::new);
        user.setCognitoSub(subject);
        user.setEmailAddress(profile.email());
        user.setFirstName(profile.given_name());
        user.setLastName(profile.family_name());
        return UserProfileResponse.from(userRepository.save(user));
    }
}
