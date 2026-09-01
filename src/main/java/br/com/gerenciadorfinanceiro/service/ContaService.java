package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.ContaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.ContaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoContasResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Usuario;

import java.math.BigDecimal;
import java.util.List;

public interface ContaService {

    List<ContaResponseDto> listarContas(Long usuarioId);

    ContaResponseDto buscarPorId(Long id, Long usuarioId);

    Conta buscarEntidadePorId(Long id, Long usuarioId);

    ContaResponseDto criar(ContaRequestDto request, Usuario usuario);

    ContaResponseDto atualizar(Long id, ContaRequestDto request, Long usuarioId);

    void excluir(Long id, Long usuarioId);

    BigDecimal calcularSaldoConsolidado(Long usuarioId);

    BigDecimal calcularSaldoInvestimentos(Long usuarioId);

    BigDecimal calcularPatrimonioTotal(Long usuarioId);

    ResumoSaldosDto obterResumoSaldos(Long usuarioId);

    ResumoContasResponseDto obterResumoContas(Long usuarioId);

    List<TransacaoResponseDto> obterExtratoDetalhado(Long contaId, Long usuarioId);

    Conta salvarEntidade(Conta conta);
}
