package br.com.fiap.pos.tech_challenge.auth.application;

import br.com.fiap.pos.tech_challenge.auth.application.port.CustomerRepository;
import br.com.fiap.pos.tech_challenge.auth.application.port.TokenIssuer;
import br.com.fiap.pos.tech_challenge.auth.domain.Cpf;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CPF = "529.982.247-25";

    @Mock
    private CustomerRepository repository;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private AuthService service;

    @Test
    void shouldIssueTokenForActiveCustomer() {
        var customer = new Customer(UUID.randomUUID(), "Maria Silva", true);
        when(repository.findByCpf(new Cpf("52998224725"))).thenReturn(Optional.of(customer));
        when(tokenIssuer.issue(customer)).thenReturn(new TokenIssuer.IssuedToken("jwt", 1800));

        var result = service.authenticate(CPF);

        assertThat(result).isEqualTo(new AuthResult.Success("jwt", 1800));
    }

    @Test
    void shouldRejectInvalidCpfWithoutQueryingDatabase() {
        var result = service.authenticate("123.456.789-00");

        assertThat(result).isInstanceOf(AuthResult.InvalidCpf.class);
        verifyNoInteractions(repository, tokenIssuer);
    }

    @Test
    void shouldRejectUnknownCustomer() {
        when(repository.findByCpf(any())).thenReturn(Optional.empty());

        var result = service.authenticate(CPF);

        assertThat(result).isEqualTo(new AuthResult.InvalidCredentials(AuthResult.Reason.CUSTOMER_NOT_FOUND));
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldRejectInactiveCustomer() {
        when(repository.findByCpf(any()))
                .thenReturn(Optional.of(new Customer(UUID.randomUUID(), "Maria Silva", false)));

        var result = service.authenticate(CPF);

        assertThat(result).isEqualTo(new AuthResult.InvalidCredentials(AuthResult.Reason.CUSTOMER_INACTIVE));
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void shouldQueryWithNormalizedDigits() {
        when(repository.findByCpf(any())).thenReturn(Optional.empty());

        service.authenticate(CPF);

        verify(repository).findByCpf(new Cpf("52998224725"));
    }
}
