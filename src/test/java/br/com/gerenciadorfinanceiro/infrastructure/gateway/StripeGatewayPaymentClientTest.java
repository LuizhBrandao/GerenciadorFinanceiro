package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.AssinaturaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.CobrancaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.ReembolsoRequest;
import br.com.gerenciadorfinanceiro.service.PagamentoGatewayService;
import br.com.gerenciadorfinanceiro.service.impl.PagamentoGatewayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StripeGatewayPaymentClientTest {

    private StripeGatewayPaymentClient gatewayClient;
    private PagamentoGatewayService pagamentoService;

    @BeforeEach
    void setUp() {
        gatewayClient = new StripeGatewayPaymentClient();
        pagamentoService = new PagamentoGatewayServiceImpl(gatewayClient);
    }

    @Test
    @DisplayName("Idempotência Outbound: Gera chave de idempotência padrão UUID v4 sem dados pessoais")
    void deveGerarChaveIdempotenciaValida() {
        String chave = gatewayClient.gerarChaveIdempotencia();

        assertNotNull(chave);
        assertDoesNotThrow(() -> UUID.fromString(chave));
        assertFalse(chave.contains("@"), "Chave não pode conter e-mail.");
        assertEquals(36, chave.length(), "UUID v4 deve ter 36 caracteres.");
    }

    @Test
    @DisplayName("Idempotência Outbound: Rejeita chaves que contenham e-mail ou CPF como dado pessoal")
    void deveRejeitarChaveComDadosPessoais() {
        CobrancaRequest request = new CobrancaRequest(new BigDecimal("150.00"), "BRL", "Cobrança Teste", "cus_1", Map.of());

        // Tentativa usando e-mail
        IllegalArgumentException exEmail = assertThrows(IllegalArgumentException.class, () ->
                gatewayClient.criarCobranca(request, "usuario@email.com"));
        assertTrue(exEmail.getMessage().contains("dados pessoais"));

        // Tentativa usando CPF (11 dígitos)
        IllegalArgumentException exCpf = assertThrows(IllegalArgumentException.class, () ->
                gatewayClient.criarCobranca(request, "12345678901"));
        assertTrue(exCpf.getMessage().contains("dados pessoais"));

        // Tentativa usando string aleatória não-UUID
        IllegalArgumentException exUuid = assertThrows(IllegalArgumentException.class, () ->
                gatewayClient.criarCobranca(request, "chave-invalida-123"));
        assertTrue(exUuid.getMessage().contains("UUID v4"));
    }

    @Test
    @DisplayName("Idempotência Outbound: Chamadas repetidas com a mesma chave retornam o MESMO PaymentIntent sem duplicar")
    void deveGarantirIdempotenciaCriacaoCobranca() {
        String chave = gatewayClient.gerarChaveIdempotencia();
        CobrancaRequest request = new CobrancaRequest(new BigDecimal("250.00"), "BRL", "Assinatura Pro", "cus_100", Map.of());

        GatewayPaymentIntent intent1 = gatewayClient.criarCobranca(request, chave);
        GatewayPaymentIntent intent2 = gatewayClient.criarCobranca(request, chave);

        assertNotNull(intent1);
        assertNotNull(intent2);
        assertEquals(intent1.id(), intent2.id(), "O ID retornado pelo Gateway deve ser exatamente o mesmo.");
        assertEquals(intent1.idempotencyKey(), intent2.idempotencyKey());
        assertEquals(new BigDecimal("250.00"), intent2.amount());
    }

    @Test
    @DisplayName("Idempotência Outbound: Chamadas repetidas com a mesma chave retornam a MESMA Assinatura sem duplicar")
    void deveGarantirIdempotenciaCriacaoAssinatura() {
        String chave = gatewayClient.gerarChaveIdempotencia();
        AssinaturaRequest request = new AssinaturaRequest("plano_anual_vip", "cus_100", Map.of());

        GatewaySubscription sub1 = gatewayClient.criarAssinatura(request, chave);
        GatewaySubscription sub2 = gatewayClient.criarAssinatura(request, chave);

        assertEquals(sub1.id(), sub2.id());
        assertEquals(sub1.idempotencyKey(), sub2.idempotencyKey());
        assertTrue(sub2.isActive());
    }

    @Test
    @DisplayName("Idempotência Outbound: Chamadas repetidas com a mesma chave retornam o MESMO Reembolso sem duplicar")
    void deveGarantirIdempotenciaCriacaoReembolso() {
        String chave = gatewayClient.gerarChaveIdempotencia();
        ReembolsoRequest request = new ReembolsoRequest("pi_origem_123", new BigDecimal("50.00"), "Duplicidade de pedido");

        GatewayRefund ref1 = gatewayClient.criarReembolso(request, chave);
        GatewayRefund ref2 = gatewayClient.criarReembolso(request, chave);

        assertEquals(ref1.id(), ref2.id());
        assertEquals(ref1.idempotencyKey(), ref2.idempotencyKey());
        assertEquals(new BigDecimal("50.00"), ref2.amount());
    }

    @Test
    @DisplayName("Idempotência e Retry: Em caso de timeout/falha transitória na rede, o retry reaproveita a MESMA chave e sucede")
    void deveReaproveitarMesmaChaveEmCasoDeRetry() {
        String chave = gatewayClient.gerarChaveIdempotencia();
        CobrancaRequest request = new CobrancaRequest(new BigDecimal("99.90"), "BRL", "Cobrança com retry", "cus_retry", Map.of());

        // Simula falha na 1ª tentativa
        gatewayClient.simularFalhaTransitoria(chave);

        // O serviço executa a 1ª tentativa (falha) e automaticamente retenta com a MESMA chave
        GatewayPaymentIntent intent = pagamentoService.processarCobrancaComRetry(request, chave);

        assertNotNull(intent);
        assertEquals(chave, intent.idempotencyKey());
        assertEquals("succeeded", intent.status());
    }
}
