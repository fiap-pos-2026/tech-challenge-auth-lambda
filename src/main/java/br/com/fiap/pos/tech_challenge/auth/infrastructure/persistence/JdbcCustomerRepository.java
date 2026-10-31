package br.com.fiap.pos.tech_challenge.auth.infrastructure.persistence;

import br.com.fiap.pos.tech_challenge.auth.application.port.CustomerRepository;
import br.com.fiap.pos.tech_challenge.auth.domain.Cpf;
import br.com.fiap.pos.tech_challenge.auth.domain.Customer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Consulta direta (somente leitura) na tabela {@code customers} do core.
 * <p>
 * Uma conexão por ambiente de execução da Lambda, reaproveitada entre invocações
 * (cada instância atende uma requisição por vez, então pool não ajuda).
 *
 * @author johncgo
 * @since 2026-10-30
 */
public class JdbcCustomerRepository implements CustomerRepository {

    // O core grava o documento como veio na requisição (com ou sem máscara), por isso a comparação só por dígitos
    private static final String FIND_BY_CPF = """
            SELECT _uuid, _name, _active
              FROM customers
             WHERE _document_type = 'CPF'
               AND regexp_replace(_document, '\\D', '', 'g') = ?
            """;

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final String url;

    private final Properties properties;

    private Connection connection;

    public JdbcCustomerRepository(String url, String username, String password) {
        this.url = url;
        this.properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "10");
        properties.setProperty("readOnly", "true");
        properties.setProperty("ApplicationName", "tech-challenge-auth-lambda");
    }

    @Override
    public Optional<Customer> findByCpf(Cpf cpf) {
        try (var statement = connection().prepareStatement(FIND_BY_CPF)) {
            statement.setString(1, cpf.digits());
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(
                        rs.getObject("_uuid", UUID.class),
                        rs.getString("_name"),
                        rs.getBoolean("_active")));
            }
        } catch (SQLException e) {
            closeQuietly();
            throw new IllegalStateException("Falha ao consultar cliente", e);
        }
    }

    private synchronized Connection connection() throws SQLException {
        if (connection == null || !connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
            closeQuietly();
            connection = DriverManager.getConnection(url, properties);
        }
        return connection;
    }

    private void closeQuietly() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignored) {
            // conexão já quebrada; a próxima chamada abre outra
        }
        connection = null;
    }
}
