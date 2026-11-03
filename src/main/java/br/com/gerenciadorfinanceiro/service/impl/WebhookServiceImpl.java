package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.WebhookReceiptResult;
import br.com.gerenciadorfinanceiro.exception.AssinaturaInvalidaException;
import br.com.gerenciadorfinanceiro.infrastructure.security.WebhookSignatureVerifier;
import br.com.gerenciadorfinanceiro.model.EventoWebhook;
import br.com.gerenciadorfinanceiro.repository.EventoWebhookRepository;
import br.com.gerenciadorfinanceiro.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private final WebhookSignatureVerifier signatureVerifier;
    private final EventoWebhookRepository eventoWebhookRepository;
    private final WebhookAsyncWorker asyncWorker;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebhookReceiptResult receberEProcessar(String rawPayload, String signatureHeader) {
        // Item 2.a: Confirma assinatura com corpo bruto antes de qualquer processamento
        signatureVerifier.verificarAssinatura(rawPayload, signatureHeader);

        String eventId;
        String tipoEvento;
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            eventId = root.path("id").asText();
            tipoEvento = root.path("type").asText("desconhecido");
        } catch (Exception e) {
            throw new AssinaturaInvalidaException("Payload JSON de webhook malformatado: " + e.getMessage(), e);
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("O payload do evento não possui o campo obrigatório 'id'.");
        }

        // Item 2.b e 2.c: Gravação Atômica First do eventId em transação dedicada (REQUIRES_NEW)
        EventoWebhook eventoGravado = tentarGravarEventoAtomicamente(eventId, "STRIPE", tipoEvento, rawPayload);

        if (eventoGravado == null) {
            // Já existia no banco ou colidiu com unique constraint
            log.info("[Webhook Inbound] Evento duplicado '{}' já registrado. Respondendo 200 OK sem reprocessar.", eventId);
            return WebhookReceiptResult.duplicado(eventId);
        }

        // Item 2.e: Responde 2xx rápido e manda trabalho pesado para processamento assíncrono
        asyncWorker.processarEventoAssincrono(eventoGravado.getId(), eventId, tipoEvento, rawPayload);

        log.info("[Webhook Inbound] Evento '{}' aceito e registrado com sucesso. Resposta 200 emitida.", eventId);
        return WebhookReceiptResult.novo(eventId);
    }

    private EventoWebhook tentarGravarEventoAtomicamente(String eventId, String gateway, String tipoEvento, String rawPayload) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            return txTemplate.execute(status -> {
                if (eventoWebhookRepository.existsByEventId(eventId)) {
                    return null;
                }
                String resumo = (rawPayload != null && rawPayload.length() > 500)
                        ? rawPayload.substring(0, 500)
                        : rawPayload;

                EventoWebhook evento = new EventoWebhook(eventId, gateway, tipoEvento, resumo);
                return eventoWebhookRepository.saveAndFlush(evento);
            });
        } catch (DataIntegrityViolationException ex) {
            log.info("[Webhook Inbound] Colisão de chave única para o evento '{}'. Considerado já registrado.", eventId);
            return null;
        }
    }
}
