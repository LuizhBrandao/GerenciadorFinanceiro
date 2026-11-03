package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import java.math.BigDecimal;
import java.util.Map;

public record GatewayPaymentIntent(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String customerId,
        Map<String, String> metadata,
        String idempotencyKey
) {
    public GatewayPaymentIntent(String id, String status, BigDecimal amount, String currency, String customerId, Map<String, String> metadata) {
        this(id, status, amount, currency, customerId, metadata, null);
    }

    public boolean isSucceeded() {
        return "succeeded".equalsIgnoreCase(this.status);
    }
}
