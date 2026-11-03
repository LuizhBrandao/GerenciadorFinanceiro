package br.com.gerenciadorfinanceiro.controller.dto;

public record WebhookReceiptResult(
        String eventId,
        boolean jaProcessado,
        String status,
        String mensagem
) {
    public static WebhookReceiptResult novo(String eventId) {
        return new WebhookReceiptResult(eventId, false, "accepted", "Evento registrado com sucesso e enfileirado para processamento.");
    }

    public static WebhookReceiptResult duplicado(String eventId) {
        return new WebhookReceiptResult(eventId, true, "already_processed", "Evento já recebido e registrado anteriormente. Nenhuma ação duplicada realizada.");
    }
}
