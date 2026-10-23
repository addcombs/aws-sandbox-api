package combs.adam.awssandboxapi;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String emailAddress,
        Instant createdDate,
        Instant updatedDate
) {

    static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailAddress(),
                user.getCreatedDate(),
                user.getUpdatedDate());
    }
}
