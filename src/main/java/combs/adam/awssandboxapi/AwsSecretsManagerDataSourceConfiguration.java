package combs.adam.awssandboxapi;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.database.password.secret.arn")
class AwsSecretsManagerDataSourceConfiguration {

    @Bean
    DataSource dataSource(
            DataSourceProperties dataSourceProperties,
            @Value("${app.database.password.secret.arn}") String secretArn) {
        if (secretArn == null || secretArn.isBlank()) {
            throw new IllegalStateException("Database password secret ARN is not configured");
        }

        String password;
        try (SecretsManagerClient client = SecretsManagerClient.create()) {
            password = client.getSecretValue(GetSecretValueRequest.builder()
                            .secretId(secretArn)
                            .build())
                    .secretString();
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Database password secret is empty");
        }

        return dataSourceProperties.initializeDataSourceBuilder()
                .password(password)
                .build();
    }
}
