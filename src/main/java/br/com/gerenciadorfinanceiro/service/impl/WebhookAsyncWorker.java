package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayApiClient;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayPaymentIntent;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewaySubscription;
import br.com.gerenciadorfinanceiro.model.EventoWebhook;
import br.com.gerenciadorfinanceiro.repository.EventoWebhookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WebhookAsyncWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookAsyncWorker.class);

    private final EventoWebhookRepository eventoWebhookRepository;
    private final GatewayApiClient gatewayApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("webhookTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processarEventoAssincrono(Long eventoDbId, String eventId, String tipoEvento, String rawPayload) {
        log.info("[Webhook Worker] Iniciando processamento em background do evento '{}' (tipo: '{}')...", eventId, tipoEvento);

        EventoWebhook evento = eventoWebhookRepository.findById(eventoDbId).orElse(null);
        if (evento == null) {
            log.warn("[Webhook Worker] Evento ID '{}' não encontrado no banco de dados para processamento.", eventoDbId);
            return;
        }

        try {
            evento.marcarComoProcessando();
            eventoWebhookRepository.saveAndFlush(evento);

            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode dataObject = root.path("data").path("object");
            String objectId = dataObject.path("id").asText();

            if (tipoEvento != null && (tipoEvento.startsWith("payment_intent.") || tipoEvento.startsWith("charge."))) {
                // Item 2.d: Não dependa da ordem de chegada. Busca o estado canônico na API do Gateway!
                String piId = objectId.startsWith("pi_") ? objectId : dataObject.path("payment_intent").asText(objectId);
                GatewayPaymentIntent paymentIntent = gatewayApiClient.buscarPaymentIntent(piId);

                log.info("[Webhook Worker] Estado canônico obtido da API do Gateway para '{}': status='{}', valor={}",
                        paymentIntent.id(), paymentIntent.status(), paymentIntent.amount());

                if (paymentIntent.isSucceeded()) {
                    log.info("[Webhook Worker] Cobrança/Pagamento '{}' confirmado com sucesso via Gateway. Efeito financeiro aplicado.", paymentIntent.id());
                } else {
                    log.warn("[Webhook Worker] PaymentIntent '{}' possui status '{}', nenhum saldo creditado.", paymentIntent.id(), paymentIntent.status());
                }

            } else if (tipoEvento != null && tipoEvento.startsWith("customer.subscription.")) {
                // Item 2.d: Busca o estado canônico da assinatura no Gateway
                GatewaySubscription sub = gatewayApiClient.buscarAssinatura(objectId);
                log.info("[Webhook Worker] Estado canônico da assinatura '{}' no Gateway: status='{}', plano='{}'",
                        sub.id(), sub.status(), sub.planId());

                if (sub.isActive()) {
                    log.info("[Webhook Worker] Assinatura '{}' ativa. Plano liberado.", sub.id());
                } else {
                    log.info("[Webhook Worker] Assinatura '{}' inativa/cancelada. Plano revogado.", sub.id());
                }
            } else {
                log.info("[Webhook Worker] Evento '{}' processado sem ação financeira adicional necessária.", tipoEvento);
            }

            evento.marcarComoProcessado();
            eventoWebhookRepository.save(evento);
            log.info("[Webhook Worker] Evento '{}' concluído e marcado como PROCESSADO.", eventId);

        } catch (Exception e) {
            log.error("[Webhook Worker] Erro ao processar evento '{}': {}", eventId, e.getMessage(), e);
            evento.marcarComoFalha(e.getMessage());
            eventoWebhookRepository.save(evento);
        }
    }
}
