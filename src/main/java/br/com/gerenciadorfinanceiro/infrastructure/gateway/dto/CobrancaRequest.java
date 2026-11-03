package br.com.gerenciadorfinanceiro.infrastructure.gateway.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CobrancaRequest(
        BigDecimal valor,
        String moeda,
        String descricao,
        String customerId,
        Map<String, String> metadata
) {
    public CobrancaRequest {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da cobrança deve ser positivo.");
        }
        if (moeda == null || moeda.trim().isEmpty()) {
            moeda = "BRL";
        }
    }
}
