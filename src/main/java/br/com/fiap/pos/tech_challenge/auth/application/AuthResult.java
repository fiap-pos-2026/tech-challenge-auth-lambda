package br.com.fiap.pos.tech_challenge.auth.application;

/**
 * Resultado da autenticação por CPF.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public sealed interface AuthResult {

    record Success(String accessToken, long expiresIn) implements AuthResult {
    }

    record InvalidCpf() implements AuthResult {
    }

    /**
     * Cliente inexistente ou inativo. Para quem chamou a resposta é a mesma;
     * o motivo real serve só para log.
     */
    record InvalidCredentials(Reason reason) implements AuthResult {
    }

    enum Reason {
        CUSTOMER_NOT_FOUND,
        CUSTOMER_INACTIVE
    }
}
