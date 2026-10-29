package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.OrcamentoResponseDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.exception.RegraNegocioException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Orcamento;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.OrcamentoRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import br.com.gerenciadorfinanceiro.service.OrcamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoServiceImpl implements OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final CategoriaService categoriaService;
    private final TransacaoRepository transacaoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrcamentoResponseDto> listarPorUsuario(Long usuarioId) {
        return orcamentoRepository.findByCategoriaUsuarioId(usuarioId).stream()
                .map(o -> {
                    BigDecimal gasto = calcularTotalGasto(o.getCategoria().getId(), o.getAno(), o.getMes(), usuarioId);
                    return DtoMapper.toOrcamentoResponse(o, gasto);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrcamentoResponseDto> listarPorCompetencia(Long usuarioId, Integer ano, Integer mes) {
        return orcamentoRepository.findByCategoriaUsuarioIdAndAnoAndMes(usuarioId, ano, mes).stream()
                .map(o -> {
                    BigDecimal gasto = calcularTotalGasto(o.getCategoria().getId(), o.getAno(), o.getMes(), usuarioId);
                    return DtoMapper.toOrcamentoResponse(o, gasto);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrcamentoResponseDto buscarPorId(Long id, Long usuarioId) {
        Orcamento orcamento = buscarEntidadePorId(id, usuarioId);
        BigDecimal gasto = calcularTotalGasto(orcamento.getCategoria().getId(), orcamento.getAno(), orcamento.getMes(), usuarioId);
        return DtoMapper.toOrcamentoResponse(orcamento, gasto);
    }

    @Override
    @Transactional(readOnly = true)
    public Orcamento buscarEntidadePorId(Long id, Long usuarioId) {
        return orcamentoRepository.findByIdAndCategoriaUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Orçamento não encontrado ou não pertence ao usuário."));
    }

    @Override
    @Transactional
    public OrcamentoResponseDto salvar(OrcamentoRequestDto request, Long usuarioId) {
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuarioId);

        if (categoria.getTipo() != TipoTransacao.DESPESA) {
            throw new RegraNegocioException("Orçamentos mensais só podem ser configurados para categorias de DESPESA.");
        }

        if (orcamentoRepository.existsByCategoriaIdAndAnoAndMes(request.categoriaId(), request.ano(), request.mes())) {
            throw new RegraNegocioException("Já existe um orçamento cadastrado para a categoria '" + categoria.getNome() +
                    "' na competência " + String.format("%02d/%d", request.mes(), request.ano()) + ".");
        }

        Orcamento orcamento = new Orcamento(
                categoria,
                request.ano(),
                request.mes(),
                request.valorLimite(),
                request.observacao()
        );

        try {
            Orcamento salvo = orcamentoRepository.save(orcamento);
            BigDecimal gasto = calcularTotalGasto(salvo.getCategoria().getId(), salvo.getAno(), salvo.getMes(), usuarioId);
            return DtoMapper.toOrcamentoResponse(salvo, gasto);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Já existe um orçamento cadastrado para a categoria '" + categoria.getNome() +
                    "' na competência " + String.format("%02d/%d", request.mes(), request.ano()) + ".");
        }
    }

    @Override
    @Transactional
    public OrcamentoResponseDto atualizar(Long id, OrcamentoRequestDto request, Long usuarioId) {
        Orcamento existente = buscarEntidadePorId(id, usuarioId);

        existente.atualizarLimite(request.valorLimite(), request.observacao());
        Orcamento salvo = orcamentoRepository.save(existente);

        BigDecimal gasto = calcularTotalGasto(salvo.getCategoria().getId(), salvo.getAno(), salvo.getMes(), usuarioId);
        return DtoMapper.toOrcamentoResponse(salvo, gasto);
    }

    @Override
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Orcamento orcamento = buscarEntidadePorId(id, usuarioId);
        orcamentoRepository.delete(orcamento);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalGasto(Long categoriaId, Integer ano, Integer mes, Long usuarioId) {
        BigDecimal total = transacaoRepository.somarDespesasPagasPorCategoriaECompetencia(usuarioId, categoriaId, ano, mes);
        return total != null ? total : BigDecimal.ZERO;
    }
}
