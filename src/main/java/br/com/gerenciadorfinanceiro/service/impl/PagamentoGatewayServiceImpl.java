package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayPaymentClient;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayPaymentIntent;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayRefund;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewaySubscription;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.AssinaturaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.CobrancaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.ReembolsoRequest;
import br.com.gerenciadorfinanceiro.service.PagamentoGatewayService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PagamentoGatewayServiceImpl implements PagamentoGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoGatewayServiceImpl.class);

    private final GatewayPaymentClient gatewayPaymentClient;

    @Override
    public GatewayPaymentIntent processarCobrancaComRetry(CobrancaRequest request, String chaveIdempotencia) {
        String chave = (chaveIdempotencia != null && !chaveIdempotencia.isBlank())
                ? chaveIdempotencia
                : gatewayPaymentClient.gerarChaveIdempotencia();

        int maxTentativas = 3;
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                log.info("[Outbound Retry] Tentativa {} de cobrança com Idempotency-Key '{}'", tentativa, chave);
                return gatewayPaymentClient.criarCobranca(request, chave);
            } catch (IllegalStateException e) {
                log.warn("[Outbound Retry] Erro transitório na tentativa {}: {}. Reenviando com a MESMA chave...", tentativa, e.getMessage());
                if (tentativa == maxTentativas) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Falha definitiva ao comunicar com o Gateway de pagamentos.");
    }

    @Override
    public GatewaySubscription processarAssinaturaComRetry(AssinaturaRequest request, String chaveIdempotencia) {
        String chave = (chaveIdempotencia != null && !chaveIdempotencia.isBlank())
                ? chaveIdempotencia
                : gatewayPaymentClient.gerarChaveIdempotencia();

        int maxTentativas = 3;
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                log.info("[Outbound Retry] Tentativa {} de assinatura com Idempotency-Key '{}'", tentativa, chave);
                return gatewayPaymentClient.criarAssinatura(request, chave);
            } catch (IllegalStateException e) {
                log.warn("[Outbound Retry] Erro transitório na tentativa {}: {}. Reenviando com a MESMA chave...", tentativa, e.getMessage());
                if (tentativa == maxTentativas) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Falha definitiva ao comunicar com o Gateway de pagamentos.");
    }

    @Override
    public GatewayRefund processarReembolsoComRetry(ReembolsoRequest request, String chaveIdempotencia) {
        String chave = (chaveIdempotencia != null && !chaveIdempotencia.isBlank())
                ? chaveIdempotencia
                : gatewayPaymentClient.gerarChaveIdempotencia();

        int maxTentativas = 3;
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                log.info("[Outbound Retry] Tentativa {} de reembolso com Idempotency-Key '{}'", tentativa, chave);
                return gatewayPaymentClient.criarReembolso(request, chave);
            } catch (IllegalStateException e) {
                log.warn("[Outbound Retry] Erro transitório na tentativa {}: {}. Reenviando com a MESMA chave...", tentativa, e.getMessage());
                if (tentativa == maxTentativas) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Falha definitiva ao comunicar com o Gateway de pagamentos.");
    }
}
