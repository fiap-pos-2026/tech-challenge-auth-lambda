package br.com.fiap.pos.tech_challenge.auth.domain;

import java.util.UUID;

/**
 * Projeção mínima do cliente do core necessária para autenticar.
 *
 * @author johncgo
 * @since 2026-10-30
 */
public record Customer(UUID uuid, String name, boolean active) {
}
