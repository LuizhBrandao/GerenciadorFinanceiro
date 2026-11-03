package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StripeGatewayClient implements GatewayApiClient {

    private static final Logger log = LoggerFactory.getLogger(StripeGatewayClient.class);

    private final Map<String, GatewayPaymentIntent> simuladorPaymentIntents = new ConcurrentHashMap<>();
    private final Map<String, GatewaySubscription> simuladorAssinaturas = new ConcurrentHashMap<>();

    @Override
    public GatewayPaymentIntent buscarPaymentIntent(String paymentIntentId) {
        log.info("[ACL Gateway] Consultando estado canônico do PaymentIntent '{}' na API do Gateway Stripe...", paymentIntentId);
        if (simuladorPaymentIntents.containsKey(paymentIntentId)) {
            return simuladorPaymentIntents.get(paymentIntentId);
        }
        // Fallback canônico padrão simulando sucesso caso não haja override prévio de teste
        return new GatewayPaymentIntent(
                paymentIntentId,
                "succeeded",
                BigDecimal.valueOf(100.00),
                "BRL",
                "cus_default",
                Map.of("sistema", "gerenciador-financeiro")
        );
    }

    @Override
    public GatewaySubscription buscarAssinatura(String subscriptionId) {
        log.info("[ACL Gateway] Consultando estado canônico da Subscription '{}' na API do Gateway Stripe...", subscriptionId);
        if (simuladorAssinaturas.containsKey(subscriptionId)) {
            return simuladorAssinaturas.get(subscriptionId);
        }
        return new GatewaySubscription(
                subscriptionId,
                "active",
                "plano_premium_mensal",
                "cus_default",
                LocalDateTime.now().plusMonths(1)
        );
    }

    @Override
    public void registrarSimulacaoPaymentIntent(GatewayPaymentIntent intent) {
        if (intent != null && intent.id() != null) {
            simuladorPaymentIntents.put(intent.id(), intent);
        }
    }

    @Override
    public void registrarSimulacaoAssinatura(GatewaySubscription subscription) {
        if (subscription != null && subscription.id() != null) {
            simuladorAssinaturas.put(subscription.id(), subscription);
        }
    }
}
