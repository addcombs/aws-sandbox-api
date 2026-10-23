package combs.adam.awssandboxapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ApiCorsConfiguration implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public ApiCorsConfiguration(
            @Value("${app.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:http://localhost:4200}}")
            String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one API CORS origin must be configured");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}
