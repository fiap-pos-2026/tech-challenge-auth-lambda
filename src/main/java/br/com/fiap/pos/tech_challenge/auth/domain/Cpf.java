package br.com.fiap.pos.tech_challenge.auth.domain;

import java.util.Optional;

/**
 * CPF já normalizado (11 dígitos) e com dígitos verificadores válidos.
 * Mesmo algoritmo do {@code TaxIdValidator} do core.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public record Cpf(String digits) {

    public Cpf {
        if (!isValid(digits)) {
            throw new IllegalArgumentException("CPF inválido");
        }
    }

    /**
     * Aceita CPF com ou sem máscara ({@code 123.456.789-09} ou {@code 12345678909}).
     */
    public static Optional<Cpf> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String digits = value.replaceAll("\\D", "");
        return isValid(digits) ? Optional.of(new Cpf(digits)) : Optional.empty();
    }

    /**
     * Versão segura para log: {@code ***.456.789-**}.
     */
    public String masked() {
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }

    @Override
    public String toString() {
        return masked();
    }

    private static boolean isValid(String cpf) {
        if (cpf == null || cpf.length() != 11 || !cpf.chars().allMatch(Character::isDigit)) return false;
        if (cpf.chars().distinct().count() == 1) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (cpf.charAt(i) - '0') * (10 - i);
        int d1 = 11 - (sum % 11);
        if (d1 >= 10) d1 = 0;
        if (d1 != cpf.charAt(9) - '0') return false;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (cpf.charAt(i) - '0') * (11 - i);
        int d2 = 11 - (sum % 11);
        if (d2 >= 10) d2 = 0;
        return d2 == cpf.charAt(10) - '0';
    }
}
