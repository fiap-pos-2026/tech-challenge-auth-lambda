package br.com.fiap.pos.tech_challenge.auth.infrastructure.security;

import br.com.fiap.pos.tech_challenge.auth.TestKeys;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenIssuerTest {

    private final RsaKeys keys = TestKeys.rsaKeys();

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(keys, "auth-lambda-test", "tech-challenge-auth-lambda",
            "tech-challenge-core", Duration.ofMinutes(30), Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void shouldIssueTokenVerifiableWithPublicKey() {
        var customer = new Customer(UUID.randomUUID(), "Maria Silva", true);

        var token = issuer.issue(customer);
        var jwt = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build().decode(token.value());

        assertThat(token.expiresIn()).isEqualTo(1800);
        assertThat(jwt.getHeaders()).containsEntry("kid", "auth-lambda-test").containsEntry("alg", "RS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("tech-challenge-auth-lambda");
        assertThat(jwt.getAudience()).containsExactly("tech-challenge-core");
        assertThat(jwt.getSubject()).isEqualTo(customer.uuid().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Maria Silva");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        assertThat(jwt.getId()).isNotBlank();
    }

    @Test
    void shouldOnlyCarryContractClaims() {
        var jwt = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build()
                .decode(issuer.issue(new Customer(UUID.randomUUID(), "Maria Silva", true)).value());

        assertThat(jwt.getClaims().keySet())
                .containsExactlyInAnyOrder("iss", "aud", "sub", "iat", "exp", "jti", "role", "name");
    }

    @Test
    void shouldPublishOnlyPublicKeyInJwks() throws Exception {
        var jwks = JWKSet.parse(issuer.publicJwks());

        assertThat(jwks.getKeys()).hasSize(1);
        var key = (RSAKey) jwks.getKeys().getFirst();
        assertThat(key.getKeyID()).isEqualTo("auth-lambda-test");
        assertThat(key.isPrivate()).isFalse();
        assertThat(key.toRSAPublicKey()).isEqualTo(keys.publicKey());
    }

    @Test
    void shouldDerivePublicKeyFromEscapedPem() {
        var escaped = TestKeys.privateKeyPem().replace("\n", "\\n");

        assertThat(RsaKeys.fromPrivateKeyPem(escaped).publicKey()).isEqualTo(keys.publicKey());
    }
}
