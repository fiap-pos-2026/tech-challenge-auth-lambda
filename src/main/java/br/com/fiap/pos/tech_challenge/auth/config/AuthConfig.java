package br.com.fiap.pos.tech_challenge.auth.config;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * Configuração da Lambda lida das variáveis de ambiente.
 * Nada sensível tem valor padrão: sem chave ou sem banco a função falha no cold start.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public record AuthConfig(
        String jdbcUrl,
        String jdbcUsername,
        String jdbcPassword,
        String jwtPrivateKey,
        String jwtKeyId,
        String jwtIssuer,
        String jwtAudience,
        Duration jwtExpiry) {

    public static final String DEFAULT_ISSUER = "tech-challenge-auth-lambda";
    public static final String DEFAULT_AUDIENCE = "tech-challenge-core";
    public static final Duration DEFAULT_EXPIRY = Duration.ofMinutes(30);

    public static AuthConfig fromEnv() {
        return from(System.getenv());
    }

    public static AuthConfig from(Map<String, String> env) {
        Function<String, String> required = name -> {
            var value = env.get(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Variável de ambiente obrigatória ausente: " + name);
            }
            return value;
        };

        return new AuthConfig(
                required.apply("JDBC_URL"),
                required.apply("JDBC_USERNAME"),
                required.apply("JDBC_PASSWORD"),
                required.apply("JWT_PRIVATE_KEY"),
                required.apply("JWT_KEY_ID"),
                env.getOrDefault("JWT_ISSUER", DEFAULT_ISSUER),
                env.getOrDefault("JWT_AUDIENCE", DEFAULT_AUDIENCE),
                env.containsKey("JWT_EXPIRY") ? Duration.parse(env.get("JWT_EXPIRY")) : DEFAULT_EXPIRY);
    }
}
