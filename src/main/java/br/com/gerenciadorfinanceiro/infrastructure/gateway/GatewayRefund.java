package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import java.math.BigDecimal;

public record GatewayRefund(
        String id,
        String paymentIntentId,
        BigDecimal amount,
        String status,
        String idempotencyKey
) {
    public boolean isSucceeded() {
        return "succeeded".equalsIgnoreCase(this.status);
    }
}
