package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import java.time.LocalDateTime;

public record GatewaySubscription(
        String id,
        String status,
        String planId,
        String customerId,
        LocalDateTime currentPeriodEnd,
        String idempotencyKey
) {
    public GatewaySubscription(String id, String status, String planId, String customerId, LocalDateTime currentPeriodEnd) {
        this(id, status, planId, customerId, currentPeriodEnd, null);
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(this.status) || "trialing".equalsIgnoreCase(this.status);
    }
}
