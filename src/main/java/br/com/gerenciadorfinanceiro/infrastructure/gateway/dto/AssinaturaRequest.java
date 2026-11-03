package br.com.gerenciadorfinanceiro.infrastructure.gateway.dto;

import java.util.Map;

public record AssinaturaRequest(
        String planoId,
        String customerId,
        Map<String, String> metadata
) {
    public AssinaturaRequest {
        if (planoId == null || planoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O ID do plano deve ser informado.");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("O ID do cliente no gateway deve ser informado.");
        }
    }
}
