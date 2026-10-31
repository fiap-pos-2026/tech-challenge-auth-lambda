package br.com.fiap.pos.tech_challenge.auth.application.port;

import br.com.fiap.pos.tech_challenge.auth.domain.Customer;

/**
 * @author johncgo
 * @since 2026-10-30
 */
public interface TokenIssuer {

    IssuedToken issue(Customer customer);

    record IssuedToken(String value, long expiresIn) {
    }
}
