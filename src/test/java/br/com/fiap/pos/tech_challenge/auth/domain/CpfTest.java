package br.com.fiap.pos.tech_challenge.auth.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", " 529.982.247-25 "})
    void shouldParseValidCpfWithOrWithoutMask(String value) {
        assertThat(Cpf.parse(value)).contains(new Cpf("52998224725"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "52998224724",      // dígito verificador errado
            "11111111111",      // todos iguais
            "5299822472",       // 10 dígitos
            "529982247250",     // 12 dígitos
            "11.222.333/0001-81" // CNPJ válido não é CPF
    })
    void shouldRejectInvalidCpf(String value) {
        assertThat(Cpf.parse(value)).isEmpty();
    }

    @Test
    void shouldNotBuildInvalidCpfDirectly() {
        assertThatThrownBy(() -> new Cpf("12345678900")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMaskCpfForLogs() {
        var cpf = new Cpf("52998224725");

        assertThat(cpf.masked()).isEqualTo("***.982.247-**");
        assertThat(cpf.toString()).doesNotContain("52998224725");
    }
}
