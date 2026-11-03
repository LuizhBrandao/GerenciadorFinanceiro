package br.com.gerenciadorfinanceiro.infrastructure.gateway;

public interface GatewayApiClient {
    GatewayPaymentIntent buscarPaymentIntent(String paymentIntentId);
    GatewaySubscription buscarAssinatura(String subscriptionId);
    void registrarSimulacaoPaymentIntent(GatewayPaymentIntent intent);
    void registrarSimulacaoAssinatura(GatewaySubscription subscription);
}
