package br.com.gerenciadorfinanceiro.config.idempotency;

public record IdempotencyCheckResult(
        boolean novo,
        boolean emProcessamento,
        boolean payloadDivergente,
        Integer responseStatusCode,
        String responseBody
) {
    public static IdempotencyCheckResult criarNovo() {
        return new IdempotencyCheckResult(true, false, false, null, null);
    }

    public static IdempotencyCheckResult criarEmProcessamento() {
        return new IdempotencyCheckResult(false, true, false, null, null);
    }

    public static IdempotencyCheckResult criarPayloadDivergente() {
        return new IdempotencyCheckResult(false, false, true, null, null);
    }

    public static IdempotencyCheckResult criarConcluido(int statusCode, String responseBody) {
        return new IdempotencyCheckResult(false, false, false, statusCode, responseBody);
    }
}
