package br.com.gerenciadorfinanceiro.infrastructure.gateway;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.AssinaturaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.CobrancaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.ReembolsoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class StripeGatewayPaymentClient implements GatewayPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(StripeGatewayPaymentClient.class);

    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern CPF_PATTERN = Pattern.compile("(^\\d{11}$)|(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})");

    private final Map<String, GatewayPaymentIntent> cobrancasPorChave = new ConcurrentHashMap<>();
    private final Map<String, GatewaySubscription> assinaturasPorChave = new ConcurrentHashMap<>();
    private final Map<String, GatewayRefund> reembolsosPorChave = new ConcurrentHashMap<>();
    private final Set<String> falhasSimuladas = ConcurrentHashMap.newKeySet();

    @Override
    public String gerarChaveIdempotencia() {
        return UUID.randomUUID().toString();
    }

    @Override
    public GatewayPaymentIntent criarCobranca(CobrancaRequest request, String idempotencyKey) {
        validarChaveIdempotencia(idempotencyKey);

        if (falhasSimuladas.remove(idempotencyKey)) {
            log.warn("[Stripe Outbound] Simulando falha transitória de rede (Timeout/503) para chave '{}'", idempotencyKey);
            throw new IllegalStateException("Timeout de conexão com o Gateway Stripe.");
        }

        if (cobrancasPorChave.containsKey(idempotencyKey)) {
            log.info("[Stripe Outbound] Idempotency-Key '{}' reconhecida pelo Gateway. Devolvendo PaymentIntent já existente.", idempotencyKey);
            return cobrancasPorChave.get(idempotencyKey);
        }

        log.info("[Stripe Outbound] Criando novo PaymentIntent no Gateway com Idempotency-Key '{}', valor={} {}",
                idempotencyKey, request.valor(), request.moeda());

        String piId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        GatewayPaymentIntent intent = new GatewayPaymentIntent(
                piId,
                "succeeded",
                request.valor(),
                request.moeda(),
                request.customerId() != null ? request.customerId() : "cus_default",
                request.metadata() != null ? request.metadata() : Map.of(),
                idempotencyKey
        );

        cobrancasPorChave.put(idempotencyKey, intent);
        return intent;
    }

    @Override
    public GatewaySubscription criarAssinatura(AssinaturaRequest request, String idempotencyKey) {
        validarChaveIdempotencia(idempotencyKey);

        if (falhasSimuladas.remove(idempotencyKey)) {
            log.warn("[Stripe Outbound] Simulando falha transitória de rede para assinatura com chave '{}'", idempotencyKey);
            throw new IllegalStateException("Timeout de conexão com o Gateway Stripe.");
        }

        if (assinaturasPorChave.containsKey(idempotencyKey)) {
            log.info("[Stripe Outbound] Idempotency-Key '{}' reconhecida pelo Gateway. Devolvendo Subscription já existente.", idempotencyKey);
            return assinaturasPorChave.get(idempotencyKey);
        }

        log.info("[Stripe Outbound] Criando nova Subscription no Gateway com Idempotency-Key '{}', plano='{}'",
                idempotencyKey, request.planoId());

        String subId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        GatewaySubscription sub = new GatewaySubscription(
                subId,
                "active",
                request.planoId(),
                request.customerId(),
                LocalDateTime.now().plusMonths(1),
                idempotencyKey
        );

        assinaturasPorChave.put(idempotencyKey, sub);
        return sub;
    }

    @Override
    public GatewayRefund criarReembolso(ReembolsoRequest request, String idempotencyKey) {
        validarChaveIdempotencia(idempotencyKey);

        if (falhasSimuladas.remove(idempotencyKey)) {
            log.warn("[Stripe Outbound] Simulando falha transitória de rede para reembolso com chave '{}'", idempotencyKey);
            throw new IllegalStateException("Timeout de conexão com o Gateway Stripe.");
        }

        if (reembolsosPorChave.containsKey(idempotencyKey)) {
            log.info("[Stripe Outbound] Idempotency-Key '{}' reconhecida pelo Gateway. Devolvendo Refund já existente.", idempotencyKey);
            return reembolsosPorChave.get(idempotencyKey);
        }

        log.info("[Stripe Outbound] Criando novo Refund no Gateway com Idempotency-Key '{}' para PaymentIntent '{}', valor={}",
                idempotencyKey, request.paymentIntentId(), request.valor());

        String refId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        GatewayRefund refund = new GatewayRefund(
                refId,
                request.paymentIntentId(),
                request.valor(),
                "succeeded",
                idempotencyKey
        );

        reembolsosPorChave.put(idempotencyKey, refund);
        return refund;
    }

    @Override
    public void simularFalhaTransitoria(String idempotencyKey) {
        if (idempotencyKey != null) {
            falhasSimuladas.add(idempotencyKey);
        }
    }

    public void validarChaveIdempotencia(String chave) {
        if (chave == null || chave.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave de idempotência (Idempotency-Key) é obrigatória para operações com o gateway.");
        }
        String chaveLimpa = chave.trim();

        if (chaveLimpa.contains("@") || CPF_PATTERN.matcher(chaveLimpa).find()) {
            throw new IllegalArgumentException("Violação de segurança: Não é permitido utilizar dados pessoais (e-mail, CPF) como chave de idempotência.");
        }

        if (!UUID_V4_PATTERN.matcher(chaveLimpa).matches()) {
            throw new IllegalArgumentException("A chave de idempotência deve ser um UUID v4 válido (ex: 550e8400-e29b-41d4-a716-446655440000).");
        }
    }
}
