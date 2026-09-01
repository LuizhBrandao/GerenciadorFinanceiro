package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.TransacaoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransacaoService {

    List<TransacaoResponseDto> listarTransacoes(Long usuarioId);

    List<TransacaoResponseDto> buscarAvancado(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal,
                                              Long categoriaId, Long contaId, BigDecimal valorMin, BigDecimal valorMax);

    TransacaoResponseDto buscarPorId(Long id, Long usuarioId);

    Transacao buscarEntidadePorId(Long id, Long usuarioId);

    TransacaoResponseDto salvar(TransacaoRequestDto request, Usuario usuario);

    Transacao salvarEntidade(Transacao transacao);

    TransacaoResponseDto atualizar(Long id, TransacaoRequestDto request, Long usuarioId);

    void excluir(Long id, Long usuarioId);

    List<TransacaoResponseDto> criarParcelamento(TransacaoRequestDto request, int quantidadeParcelas, Usuario usuario);
}
