package br.com.fiap.pos.tech_challenge.auth;

import br.com.fiap.pos.tech_challenge.auth.application.AuthResult;
import br.com.fiap.pos.tech_challenge.auth.application.AuthService;
import br.com.fiap.pos.tech_challenge.auth.config.AuthConfig;
import br.com.fiap.pos.tech_challenge.auth.infrastructure.persistence.JdbcCustomerRepository;
import br.com.fiap.pos.tech_challenge.auth.infrastructure.security.JwtTokenIssuer;
import br.com.fiap.pos.tech_challenge.auth.infrastructure.security.RsaKeys;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada da Lambda (API Gateway HTTP API, payload 2.0).
 * <ul>
 *     <li>{@code POST /auth} — CPF → JWT de cliente</li>
 *     <li>{@code GET /auth/.well-known/jwks.json} — chave pública</li>
 *     <li>{@code GET /auth/.well-known/openid-configuration} — descoberta para o authorizer JWT do Gateway</li>
 * </ul>
 * Handler: {@code br.com.fiap.pos.tech_challenge.auth.AuthHandler::handleRequest}
 *
 * @author johncgo
 * @since 2026-10-30
 */
public class AuthHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";

    private static final Map<String, String> JSON_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Cache-Control", "no-store");

    private final AuthService service;

    private final JwtTokenIssuer tokenIssuer;

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * Usado pelo runtime da Lambda. Roda uma vez por ambiente de execução (cold start).
     */
    public AuthHandler() {
        this(AuthConfig.fromEnv());
    }

    AuthHandler(AuthConfig config) {
        this.tokenIssuer = new JwtTokenIssuer(
                RsaKeys.fromPrivateKeyPem(config.jwtPrivateKey()),
                config.jwtKeyId(),
                config.jwtIssuer(),
                config.jwtAudience(),
                config.jwtExpiry(),
                Clock.systemUTC());
        this.service = new AuthService(
                new JdbcCustomerRepository(config.jdbcUrl(), config.jdbcUsername(), config.jdbcPassword()),
                tokenIssuer);
    }

    AuthHandler(AuthService service, JwtTokenIssuer tokenIssuer) {
        this.service = service;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        var method = event.getRequestContext() != null && event.getRequestContext().getHttp() != null
                ? event.getRequestContext().getHttp().getMethod()
                : null;
        var path = event.getRawPath() == null ? "" : event.getRawPath();

        try {
            if ("GET".equals(method) && path.endsWith(JWKS_PATH)) {
                return response(200, tokenIssuer.publicJwks());
            }
            if ("GET".equals(method) && path.endsWith(DISCOVERY_PATH)) {
                return json(200, discovery());
            }
            if ("POST".equals(method) && path.endsWith("/auth")) {
                return authenticate(event, context);
            }
            return error(404, "NOT_FOUND", "Rota não encontrada");
        } catch (RuntimeException e) {
            log(context, "event=auth_error error=" + e.getClass().getSimpleName() + " message=" + e.getMessage());
            return error(500, "INTERNAL_ERROR", "Erro interno");
        }
    }

    private APIGatewayV2HTTPResponse authenticate(APIGatewayV2HTTPEvent event, Context context) {
        String cpf;
        try {
            var request = mapper.readValue(body(event), AuthRequest.class);
            cpf = request == null ? null : request.cpf();
        } catch (JacksonException | IllegalArgumentException e) {
            cpf = null;
        }

        var result = service.authenticate(cpf);
        return switch (result) {
            case AuthResult.Success success -> {
                log(context, "event=auth_success");
                yield json(200, Map.of(
                        "access_token", success.accessToken(),
                        "token_type", "Bearer",
                        "expires_in", success.expiresIn()));
            }
            case AuthResult.InvalidCpf ignored -> {
                log(context, "event=auth_rejected reason=INVALID_CPF");
                yield error(400, "INVALID_CPF", "CPF inválido");
            }
            case AuthResult.InvalidCredentials invalid -> {
                log(context, "event=auth_rejected reason=" + invalid.reason());
                yield error(401, "INVALID_CREDENTIALS", "Não foi possível autenticar com este CPF");
            }
        };
    }

    private Map<String, Object> discovery() {
        var discovery = new LinkedHashMap<String, Object>();
        discovery.put("issuer", tokenIssuer.issuer());
        discovery.put("jwks_uri", tokenIssuer.issuer() + JWKS_PATH);
        discovery.put("id_token_signing_alg_values_supported", new String[]{"RS256"});
        return discovery;
    }

    private static String body(APIGatewayV2HTTPEvent event) {
        var body = event.getBody();
        if (body == null) return "";
        return event.getIsBase64Encoded()
                ? new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8)
                : body;
    }

    private APIGatewayV2HTTPResponse error(int status, String code, String message) {
        return json(status, Map.of("error", code, "message", message));
    }

    private APIGatewayV2HTTPResponse json(int status, Object body) {
        return response(status, mapper.writeValueAsString(body));
    }

    private static APIGatewayV2HTTPResponse response(int status, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(JSON_HEADERS)
                .withBody(body)
                .build();
    }

    // Nunca logar CPF nem token
    private static void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message + System.lineSeparator());
        }
    }

    record AuthRequest(String cpf) {
    }
}
