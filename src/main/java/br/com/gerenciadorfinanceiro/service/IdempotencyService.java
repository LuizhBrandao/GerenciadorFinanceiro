package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.config.idempotency.IdempotencyCheckResult;

public interface IdempotencyService {
    String calcularHash(String payload);
    void validarChave(String chave);
    IdempotencyCheckResult verificarOuIniciar(String chave, Long usuarioId, String metodoHttp, String uriEndpoint, String requestHash);
    void registrarConclusao(String chave, int statusCode, String responseBody);
    void registrarFalha(String chave, int statusCode, String responseBody);
}
