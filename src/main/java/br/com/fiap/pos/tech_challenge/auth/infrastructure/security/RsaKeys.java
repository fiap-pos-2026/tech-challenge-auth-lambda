package br.com.fiap.pos.tech_challenge.auth.infrastructure.security;

import org.springframework.security.converter.RsaKeyConverters;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

/**
 * Carrega o par RSA a partir do PEM da chave privada (PKCS#8, {@code BEGIN PRIVATE KEY}).
 * A pública é derivada da privada, então só um secret precisa ser configurado.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public record RsaKeys(RSAPublicKey publicKey, RSAPrivateKey privateKey) {

    public static RsaKeys fromPrivateKeyPem(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Chave privada do JWT não configurada");
        }
        // Secrets em variável de ambiente às vezes chegam com "\n" literal
        var normalized = pem.replace("\\n", "\n").trim();
        var privateKey = RsaKeyConverters.pkcs8()
                .convert(new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8)));
        return new RsaKeys(derivePublicKey(privateKey), privateKey);
    }

    private static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            throw new IllegalStateException("Chave privada RSA sem parâmetros CRT");
        }
        try {
            var spec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao derivar a chave pública", e);
        }
    }
}
