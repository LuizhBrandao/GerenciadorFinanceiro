package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.AssinaturaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.CobrancaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.ReembolsoRequest;

public interface GatewayPaymentClient {
    String gerarChaveIdempotencia();
    GatewayPaymentIntent criarCobranca(CobrancaRequest request, String idempotencyKey);
    GatewaySubscription criarAssinatura(AssinaturaRequest request, String idempotencyKey);
    GatewayRefund criarReembolso(ReembolsoRequest request, String idempotencyKey);
    void simularFalhaTransitoria(String idempotencyKey);
}
