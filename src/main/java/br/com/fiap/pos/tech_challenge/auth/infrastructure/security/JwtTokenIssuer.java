package br.com.fiap.pos.tech_challenge.auth.infrastructure.security;

import br.com.fiap.pos.tech_challenge.auth.application.port.TokenIssuer;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Emite o JWT de cliente (RS256) no mesmo estilo do {@code TokenUtility} do core.
 * Nunca inclui CPF ou dados de contato: o token é só assinado, não cifrado.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public class JwtTokenIssuer implements TokenIssuer {

    public static final String ROLE = "CUSTOMER";

    private final JwtEncoder encoder;

    private final RSAKey jwk;

    private final String issuer;

    private final String audience;

    private final Duration expiryTime;

    private final Clock clock;

    public JwtTokenIssuer(RsaKeys keys, String keyId, String issuer, String audience, Duration expiryTime, Clock clock) {
        this.jwk = new RSAKey.Builder(keys.publicKey()).privateKey(keys.privateKey()).keyID(keyId).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        this.issuer = issuer;
        this.audience = audience;
        this.expiryTime = expiryTime;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(Customer customer) {
        var currentInstant = clock.instant();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(currentInstant)
                .expiresAt(currentInstant.plus(expiryTime))
                .subject(customer.uuid().toString())
                .id(UUID.randomUUID().toString())
                .claim("role", ROLE)
                .claim("name", customer.name())
                .build();

        var header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(jwk.getKeyID())
                .build();

        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiryTime.toSeconds());
    }

    /**
     * JWKS só com a chave pública, para o authorizer do API Gateway e para o core.
     */
    public String publicJwks() {
        return new JWKSet(jwk.toPublicJWK()).toString();
    }

    public String issuer() {
        return issuer;
    }
}
