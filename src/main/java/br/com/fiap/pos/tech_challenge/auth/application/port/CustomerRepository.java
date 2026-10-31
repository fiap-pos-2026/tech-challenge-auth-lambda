package br.com.fiap.pos.tech_challenge.auth.application.port;

import br.com.fiap.pos.tech_challenge.auth.domain.Cpf;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;

import java.util.Optional;

/**
 * @author johncgo
 * @since 2026-10-30
 */
public interface CustomerRepository {

    Optional<Customer> findByCpf(Cpf cpf);
}
