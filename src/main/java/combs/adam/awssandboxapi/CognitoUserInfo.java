package combs.adam.awssandboxapi;

public record CognitoUserInfo(
        String sub,
        String email,
        Boolean email_verified,
        String given_name,
        String family_name
) {
}
