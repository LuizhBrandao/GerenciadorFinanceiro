package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.TransferenciaRequestDto;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferenciaServiceImpl implements TransferenciaService {

    private final ContaService contaService;

    @Override
    @Transactional
    public void transferir(Long usuarioId, TransferenciaRequestDto request) {
        transferir(usuarioId, request.contaOrigemId(), request.contaDestinoId(), request.valor());
    }

    @Override
    @Transactional
    public void transferir(Long usuarioId, Long contaOrigemId, Long contaDestinoId, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser maior que zero.");
        }

        if (contaOrigemId == null || contaDestinoId == null) {
            throw new IllegalArgumentException("Contas de origem e destino são obrigatórias.");
        }

        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("As contas de origem e destino devem ser diferentes.");
        }

        Conta origem = contaService.buscarEntidadePorId(contaOrigemId, usuarioId);
        Conta destino = contaService.buscarEntidadePorId(contaDestinoId, usuarioId);

        origem.debitar(valor);
        destino.creditar(valor);

        contaService.salvarEntidade(origem);
        contaService.salvarEntidade(destino);
    }
}
