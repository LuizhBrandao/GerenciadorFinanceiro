package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoResponseDto;
import br.com.gerenciadorfinanceiro.model.Orcamento;

import java.math.BigDecimal;
import java.util.List;

public interface OrcamentoService {

    List<OrcamentoResponseDto> listarPorUsuario(Long usuarioId);

    List<OrcamentoResponseDto> listarPorCompetencia(Long usuarioId, Integer ano, Integer mes);

    OrcamentoResponseDto buscarPorId(Long id, Long usuarioId);

    Orcamento buscarEntidadePorId(Long id, Long usuarioId);

    OrcamentoResponseDto salvar(OrcamentoRequestDto request, Long usuarioId);

    OrcamentoResponseDto atualizar(Long id, OrcamentoRequestDto request, Long usuarioId);

    void excluir(Long id, Long usuarioId);

    BigDecimal calcularTotalGasto(Long categoriaId, Integer ano, Integer mes, Long usuarioId);
}
