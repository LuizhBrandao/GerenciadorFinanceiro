package br.com.gerenciadorfinanceiro.controller;

import br.com.gerenciadorfinanceiro.infrastructure.security.WebhookSignatureVerifier;
import br.com.gerenciadorfinanceiro.repository.EventoWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookSignatureVerifier verifier;

    @Autowired
    private EventoWebhookRepository eventoWebhookRepository;

    @Value("${webhook.stripe.secret:whsec_test_secret_for_unit_tests_12345}")
    private String secret;

    @BeforeEach
    void setUp() {
        eventoWebhookRepository.deleteAll();
    }

    @Test
    @DisplayName("Webhook: Rejeita com HTTP 400 requisição com assinatura inválida antes de qualquer gravação")
    void deveRejeitarAssinaturaInvalida() throws Exception {
        String payload = "{\"id\":\"evt_fake_999\",\"type\":\"payment_intent.succeeded\"}";

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1234567,v1=assinatura_falsa")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertEquals(0, eventoWebhookRepository.count(), "Nenhum evento deve ser persistido em caso de assinatura inválida.");
    }

    @Test
    @DisplayName("Idempotência Sequencial: Enviar o mesmo evento de webhook 2 vezes resulta em HTTP 200 e efeito executado apenas UMA vez")
    void deveAceitarRepeticaoSequencialSemEfeitoDuplicado() throws Exception {
        String eventId = "evt_sequencial_12345";
        String payload = "{\"id\":\"" + eventId + "\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_123\",\"amount\":5000}}}";
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = verifier.gerarAssinaturaHeader(payload, timestamp, secret);

        // 1ª Chamada: Novo evento
        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signatureHeader)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.jaProcessado").value(false))
                .andExpect(jsonPath("$.status").value("accepted"));

        // 2ª Chamada: Repetição do mesmo evento (retry de rede do gateway)
        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signatureHeader)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.jaProcessado").value(true))
                .andExpect(jsonPath("$.status").value("already_processed"));

        // Verificação no banco de dados: apenas 1 registro único persistido!
        assertEquals(1, eventoWebhookRepository.count(), "Deve existir exatamente UM registro do evento no banco de dados.");
    }

    @Test
    @DisplayName("Idempotência Concorrente: Enviar o mesmo evento 2 vezes ao mesmo tempo resulta em HTTP 200 e apenas UMA gravação física")
    void deveAceitarRepeticaoConcorrenteSimultanea() throws InterruptedException {
        String eventId = "evt_concorrente_99999";
        String payload = "{\"id\":\"" + eventId + "\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"id\":\"pi_conc_1\",\"amount\":15000}}}";
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = verifier.gerarAssinaturaHeader(payload, timestamp, secret);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchFim = new CountDownLatch(threads);

        AtomicInteger status200Count = new AtomicInteger(0);
        AtomicInteger errosCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    var mvcResult = mockMvc.perform(post("/webhooks/stripe")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Stripe-Signature", signatureHeader)
                                    .content(payload))
                            .andReturn();

                    if (mvcResult.getResponse().getStatus() == 200) {
                        status200Count.incrementAndGet();
                    } else {
                        errosCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errosCount.incrementAndGet();
                } finally {
                    latchFim.countDown();
                }
            });
        }

        latchInicio.countDown();
        latchFim.await();
        executor.shutdown();

        // Ambas as chamadas simultâneas devem responder HTTP 200 com sucesso ao gateway
        assertEquals(2, status200Count.get(), "Ambas as requisições simultâneas devem responder HTTP 200 OK.");
        assertEquals(0, errosCount.get(), "Nenhum erro 500 ou não tratado deve ocorrer na corrida concorrente.");

        // Verificação no banco: apenas 1 evento gravado!
        assertEquals(1, eventoWebhookRepository.count(), "Exatamente UM evento físico deve ter sido persistido no banco.");
    }
}
