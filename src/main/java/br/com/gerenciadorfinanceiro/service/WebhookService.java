package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.WebhookReceiptResult;

public interface WebhookService {
    WebhookReceiptResult receberEProcessar(String rawPayload, String signatureHeader);
}
