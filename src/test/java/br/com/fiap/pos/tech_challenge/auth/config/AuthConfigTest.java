package br.com.fiap.pos.tech_challenge.auth.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthConfigTest {

    private final Map<String, String> env = new HashMap<>(Map.of(
            "JDBC_URL", "jdbc:postgresql://localhost:5432/techbase",
            "JDBC_USERNAME", "techdev",
            "JDBC_PASSWORD", "secret",
            "JWT_PRIVATE_KEY", "pem",
            "JWT_KEY_ID", "auth-lambda-local"));

    @Test
    void shouldApplyContractDefaults() {
        var config = AuthConfig.from(env);

        assertThat(config.jwtIssuer()).isEqualTo("tech-challenge-auth-lambda");
        assertThat(config.jwtAudience()).isEqualTo("tech-challenge-core");
        assertThat(config.jwtExpiry()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void shouldOverrideOptionalValues() {
        env.put("JWT_ISSUER", "https://gateway.example/auth");
        env.put("JWT_EXPIRY", "PT15M");

        var config = AuthConfig.from(env);

        assertThat(config.jwtIssuer()).isEqualTo("https://gateway.example/auth");
        assertThat(config.jwtExpiry()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void shouldFailFastWithoutPrivateKey() {
        env.remove("JWT_PRIVATE_KEY");

        assertThatThrownBy(() -> AuthConfig.from(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY");
    }
}
