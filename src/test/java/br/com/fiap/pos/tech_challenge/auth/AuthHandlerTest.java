package br.com.fiap.pos.tech_challenge.auth;

import br.com.fiap.pos.tech_challenge.auth.application.AuthService;
import br.com.fiap.pos.tech_challenge.auth.application.port.CustomerRepository;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;
import br.com.fiap.pos.tech_challenge.auth.infrastructure.security.JwtTokenIssuer;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthHandlerTest {

    private static final String VALID_CPF = "529.982.247-25";

    private final JsonMapper mapper = JsonMapper.builder().build();

    private CustomerRepository repository;

    private AuthHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        var issuer = new JwtTokenIssuer(TestKeys.rsaKeys(), "auth-lambda-test",
                "https://gateway.example/auth", "tech-challenge-core", Duration.ofMinutes(30), Clock.systemUTC());
        handler = new AuthHandler(new AuthService(repository, issuer), issuer);
    }

    @Test
    void shouldReturnTokenForActiveCustomer() {
        when(repository.findByCpf(any())).thenReturn(Optional.of(new Customer(UUID.randomUUID(), "Maria", true)));

        var response = handler.handleRequest(post("{\"cpf\":\"" + VALID_CPF + "\"}"), null);
        var body = json(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(body.get("token_type").asString()).isEqualTo("Bearer");
        assertThat(body.get("expires_in").asLong()).isEqualTo(1800);
        assertThat(body.get("access_token").asString()).isNotBlank();
    }

    @Test
    void shouldAcceptBase64EncodedBody() {
        when(repository.findByCpf(any())).thenReturn(Optional.of(new Customer(UUID.randomUUID(), "Maria", true)));
        var event = post(Base64.getEncoder()
                .encodeToString(("{\"cpf\":\"" + VALID_CPF + "\"}").getBytes(StandardCharsets.UTF_8)));
        event.setIsBase64Encoded(true);

        assertThat(handler.handleRequest(event, null).getStatusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"cpf\":\"123.456.789-00\"}", "{}", "", "null", "not-json", "{\"cpf\":123}"})
    void shouldReturnBadRequestForInvalidCpfOrBody(String body) {
        var response = handler.handleRequest(post(body), null);

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(json(response.getBody()).get("error").asString()).isEqualTo("INVALID_CPF");
    }

    @Test
    void shouldReturnSameUnauthorizedForUnknownAndInactiveCustomer() {
        when(repository.findByCpf(any())).thenReturn(Optional.empty());
        var unknown = handler.handleRequest(post("{\"cpf\":\"" + VALID_CPF + "\"}"), null);

        when(repository.findByCpf(any())).thenReturn(Optional.of(new Customer(UUID.randomUUID(), "Maria", false)));
        var inactive = handler.handleRequest(post("{\"cpf\":\"" + VALID_CPF + "\"}"), null);

        assertThat(unknown.getStatusCode()).isEqualTo(401);
        assertThat(inactive.getStatusCode()).isEqualTo(401);
        assertThat(inactive.getBody()).isEqualTo(unknown.getBody());
    }

    @Test
    void shouldReturnInternalErrorWhenDatabaseFails() {
        when(repository.findByCpf(any())).thenThrow(new IllegalStateException("Falha ao consultar cliente"));

        var response = handler.handleRequest(post("{\"cpf\":\"" + VALID_CPF + "\"}"), null);

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(json(response.getBody()).get("error").asString()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void shouldServePublicJwks() {
        var response = handler.handleRequest(get("/auth/.well-known/jwks.json"), null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        var key = json(response.getBody()).get("keys").get(0);
        assertThat(key.get("kid").asString()).isEqualTo("auth-lambda-test");
        assertThat(key.has("d")).isFalse();
    }

    @Test
    void shouldServeDiscoveryDocument() {
        var response = handler.handleRequest(get("/auth/.well-known/openid-configuration"), null);
        var body = json(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(body.get("issuer").asString()).isEqualTo("https://gateway.example/auth");
        assertThat(body.get("jwks_uri").asString()).isEqualTo("https://gateway.example/auth/.well-known/jwks.json");
    }

    @Test
    void shouldReturnNotFoundForUnknownRoute() {
        assertThat(handler.handleRequest(get("/auth/qualquer"), null).getStatusCode()).isEqualTo(404);
    }

    private JsonNode json(String body) {
        return mapper.readTree(body);
    }

    private static APIGatewayV2HTTPEvent post(String body) {
        var event = event("POST", "/auth");
        event.setBody(body);
        return event;
    }

    private static APIGatewayV2HTTPEvent get(String path) {
        return event("GET", path);
    }

    private static APIGatewayV2HTTPEvent event(String method, String path) {
        var http = APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(method).withPath(path).build();
        var context = APIGatewayV2HTTPEvent.RequestContext.builder().withHttp(http).build();
        return APIGatewayV2HTTPEvent.builder().withRawPath(path).withRequestContext(context).build();
    }
}
