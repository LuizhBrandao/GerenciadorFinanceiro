package br.com.gerenciadorfinanceiro.infrastructure.gateway.dto;

import java.math.BigDecimal;

public record ReembolsoRequest(
        String paymentIntentId,
        BigDecimal valor,
        String motivo
) {
    public ReembolsoRequest {
        if (paymentIntentId == null || paymentIntentId.trim().isEmpty()) {
            throw new IllegalArgumentException("O ID da cobrança original a ser reembolsada deve ser informado.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do reembolso deve ser positivo.");
        }
    }
}
