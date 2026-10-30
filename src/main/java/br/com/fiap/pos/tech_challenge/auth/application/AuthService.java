package br.com.fiap.pos.tech_challenge.auth.application;

import br.com.fiap.pos.tech_challenge.auth.application.port.CustomerRepository;
import br.com.fiap.pos.tech_challenge.auth.application.port.TokenIssuer;
import br.com.fiap.pos.tech_challenge.auth.domain.Cpf;

/**
 * Fluxo de autenticação do cliente: valida o CPF, consulta o cliente e emite o JWT.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public class AuthService {

    private final CustomerRepository repository;

    private final TokenIssuer tokenIssuer;

    public AuthService(CustomerRepository repository, TokenIssuer tokenIssuer) {
        this.repository = repository;
        this.tokenIssuer = tokenIssuer;
    }

    public AuthResult authenticate(String rawCpf) {
        var cpf = Cpf.parse(rawCpf);
        if (cpf.isEmpty()) {
            return new AuthResult.InvalidCpf();
        }

        var customer = repository.findByCpf(cpf.get());
        if (customer.isEmpty()) {
            return new AuthResult.InvalidCredentials(AuthResult.Reason.CUSTOMER_NOT_FOUND);
        }
        if (!customer.get().active()) {
            return new AuthResult.InvalidCredentials(AuthResult.Reason.CUSTOMER_INACTIVE);
        }

        var token = tokenIssuer.issue(customer.get());
        return new AuthResult.Success(token.value(), token.expiresIn());
    }
}
