package combs.adam.awssandboxapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiHealthController {

    @GetMapping("/api/health")
    public Map<String, String> getHealth() {
        return Map.of("status", "UP");
    }
}
