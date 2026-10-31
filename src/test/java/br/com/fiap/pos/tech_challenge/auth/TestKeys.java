package br.com.fiap.pos.tech_challenge.auth;

import br.com.fiap.pos.tech_challenge.auth.infrastructure.security.RsaKeys;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Par RSA exclusivo de teste ({@code src/test/resources/certs}). Nunca usar fora dos testes.
 */
public final class TestKeys {

    private TestKeys() {
    }

    public static String privateKeyPem() {
        try (var in = TestKeys.class.getResourceAsStream("/certs/test-private.pem")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static RsaKeys rsaKeys() {
        return RsaKeys.fromPrivateKeyPem(privateKeyPem());
    }
}
