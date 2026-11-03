package br.com.gerenciadorfinanceiro.infrastructure.security;

import br.com.gerenciadorfinanceiro.exception.AssinaturaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureVerifierTest {

    private WebhookSignatureVerifier verifier;
    private final String secret = "whsec_chave_de_teste_segura_123456";

    @BeforeEach
    void setUp() {
        verifier = new WebhookSignatureVerifier();
        ReflectionTestUtils.setField(verifier, "defaultSecret", secret);
        ReflectionTestUtils.setField(verifier, "defaultToleranceSeconds", 300L);
    }

    @Test
    @DisplayName("Deve validar com sucesso assinatura Stripe legítima com corpo bruto")
    void deveValidarAssinaturaComSucesso() {
        String rawBody = "{\"id\":\"evt_test_123\",\"type\":\"payment_intent.succeeded\"}";
        long timestamp = Instant.now().getEpochSecond();
        String header = verifier.gerarAssinaturaHeader(rawBody, timestamp, secret);

        assertDoesNotThrow(() -> verifier.verificarAssinatura(rawBody, header, secret, 300));
    }

    @Test
    @DisplayName("Deve rejeitar requisição caso o corpo bruto (raw body) tenha sido adulterado")
    void deveRejeitarQuandoCorpoAdulterado() {
        String rawBodyOriginal = "{\"id\":\"evt_test_123\",\"amount\":1000}";
        String rawBodyAdulterado = "{\"id\":\"evt_test_123\",\"amount\":9999}";
        long timestamp = Instant.now().getEpochSecond();
        String header = verifier.gerarAssinaturaHeader(rawBodyOriginal, timestamp, secret);

        AssinaturaInvalidaException ex = assertThrows(AssinaturaInvalidaException.class, () ->
                verifier.verificarAssinatura(rawBodyAdulterado, header, secret, 300));

        assertTrue(ex.getMessage().contains("adulterado") || ex.getMessage().contains("inválida"));
    }

    @Test
    @DisplayName("Deve rejeitar requisição se o cabeçalho de assinatura estiver ausente")
    void deveRejeitarSemCabecalho() {
        assertThrows(AssinaturaInvalidaException.class, () ->
                verifier.verificarAssinatura("{}", null, secret, 300));
        assertThrows(AssinaturaInvalidaException.class, () ->
                verifier.verificarAssinatura("{}", "   ", secret, 300));
    }

    @Test
    @DisplayName("Deve rejeitar requisição se o timestamp estiver fora da tolerância de tempo")
    void deveRejeitarTimestampExpirado() {
        String rawBody = "{\"id\":\"evt_test_expired\"}";
        long timestampAntigo = Instant.now().getEpochSecond() - 600; // 10 minutos atrás
        String header = verifier.gerarAssinaturaHeader(rawBody, timestampAntigo, secret);

        assertThrows(AssinaturaInvalidaException.class, () ->
                verifier.verificarAssinatura(rawBody, header, secret, 300));
    }
}
