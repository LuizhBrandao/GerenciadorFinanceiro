package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import java.math.BigDecimal;
import java.util.Map;

public record GatewayPaymentIntent(
        String id,
        String status,
        BigDecimal amount,
        String currency,
        String customerId,
        Map<String, String> metadata
) {
    public boolean isSucceeded() {
        return "succeeded".equalsIgnoreCase(this.status);
    }
}
