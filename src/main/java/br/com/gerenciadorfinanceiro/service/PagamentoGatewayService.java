package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayPaymentIntent;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewayRefund;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.GatewaySubscription;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.AssinaturaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.CobrancaRequest;
import br.com.gerenciadorfinanceiro.infrastructure.gateway.dto.ReembolsoRequest;

public interface PagamentoGatewayService {
    GatewayPaymentIntent processarCobrancaComRetry(CobrancaRequest request, String chaveIdempotencia);
    GatewaySubscription processarAssinaturaComRetry(AssinaturaRequest request, String chaveIdempotencia);
    GatewayRefund processarReembolsoComRetry(ReembolsoRequest request, String chaveIdempotencia);
}
