package br.com.fiap.pos.tech_challenge.auth.infrastructure.persistence;

import br.com.fiap.pos.tech_challenge.auth.domain.Cpf;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda contra um Postgres real com o schema de {@code customers} do core
 * (002-create-customers + coluna {@code _active}).
 */
@Testcontainers
class JdbcCustomerRepositoryIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static final UUID ACTIVE = UUID.randomUUID();
    private static final UUID INACTIVE = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();

    @BeforeAll
    static void createSchema() throws SQLException {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE customers (
                        _id BIGSERIAL PRIMARY KEY,
                        _uuid UUID NOT NULL UNIQUE,
                        _document_type VARCHAR(10) NOT NULL,
                        _document VARCHAR(20) NOT NULL UNIQUE,
                        _name VARCHAR(255) NOT NULL,
                        _email VARCHAR(255) NOT NULL,
                        _phone VARCHAR(50),
                        _created_at TIMESTAMP NOT NULL,
                        _version BIGINT,
                        _active BOOLEAN NOT NULL DEFAULT TRUE
                    )""");
            statement.execute("""
                    INSERT INTO customers (_uuid, _document_type, _document, _name, _email, _created_at, _active) VALUES
                      ('%s', 'CPF',  '529.982.247-25',     'Maria Silva', 'maria@example.com', now(), true),
                      ('%s', 'CPF',  '11144477735',        'João Souza',  'joao@example.com',  now(), false),
                      ('%s', 'CNPJ', '11.222.333/0001-81', 'Oficina SA',  'sa@example.com',    now(), true)
                    """.formatted(ACTIVE, INACTIVE, COMPANY));
        }
    }

    private final JdbcCustomerRepository repository =
            new JdbcCustomerRepository(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

    @Test
    void shouldFindCustomerStoredWithMask() {
        assertThat(repository.findByCpf(new Cpf("52998224725")))
                .contains(new Customer(ACTIVE, "Maria Silva", true));
    }

    @Test
    void shouldFindInactiveCustomerStoredWithoutMask() {
        assertThat(repository.findByCpf(new Cpf("11144477735")))
                .contains(new Customer(INACTIVE, "João Souza", false));
    }

    @Test
    void shouldReturnEmptyForUnknownCpf() {
        assertThat(repository.findByCpf(new Cpf("39053344705"))).isEmpty();
    }

    @Test
    void shouldReuseConnectionAcrossCalls() {
        repository.findByCpf(new Cpf("52998224725"));

        assertThat(repository.findByCpf(new Cpf("52998224725"))).isPresent();
    }

    @Test
    void shouldWrapDatabaseFailures() {
        var broken = new JdbcCustomerRepository(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), "senha-errada");

        assertThatThrownBy(() -> broken.findByCpf(new Cpf("52998224725")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao consultar cliente");
    }
}
