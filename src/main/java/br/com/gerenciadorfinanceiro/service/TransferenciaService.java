package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.TransferenciaRequestDto;

import java.math.BigDecimal;

public interface TransferenciaService {

    void transferir(Long usuarioId, TransferenciaRequestDto request);

    void transferir(Long usuarioId, Long contaOrigemId, Long contaDestinoId, BigDecimal valor);
}
