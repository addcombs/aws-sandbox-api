package combs.adam.awssandboxapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.auth.issuer-uri=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "app.auth.client-id=test-client",
        "app.auth.user-info-uri=https://test.auth.us-east-1.amazoncognito.com/oauth2/userInfo"
})
class ApiContextStartupTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startsWithCognitoUserInfoClientConfigured() {
        assertTrue(applicationContext.containsBean("cognitoUserInfoClient"));
    }
}
