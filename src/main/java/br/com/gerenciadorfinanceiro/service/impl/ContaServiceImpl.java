package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.ContaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.ContaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoContasResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.ContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaServiceImpl implements ContaService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ContaResponseDto> listarContas(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId).stream()
                .map(DtoMapper::toContaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContaResponseDto buscarPorId(Long id, Long usuarioId) {
        return DtoMapper.toContaResponse(buscarEntidadePorId(id, usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public Conta buscarEntidadePorId(Long id, Long usuarioId) {
        return contaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta não encontrada ou não pertence ao usuário."));
    }

    @Override
    @Transactional
    public ContaResponseDto criar(ContaRequestDto request, Usuario usuario) {
        BigDecimal saldoInicial = (request.saldo() != null) ? request.saldo() : BigDecimal.ZERO;
        Conta conta = new Conta(
                usuario,
                request.nome(),
                request.instituicaoFinanceira(),
                request.tipoConta(),
                saldoInicial
        );
        Conta salva = contaRepository.save(conta);
        return DtoMapper.toContaResponse(salva);
    }

    @Override
    @Transactional
    public ContaResponseDto atualizar(Long id, ContaRequestDto request, Long usuarioId) {
        Conta existente = buscarEntidadePorId(id, usuarioId);
        existente.setNome(request.nome());
        existente.setInstituicaoFinanceira(request.instituicaoFinanceira());
        existente.setTipoConta(request.tipoConta());
        
        Conta salva = contaRepository.save(existente);
        return DtoMapper.toContaResponse(salva);
    }

    @Override
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Conta conta = buscarEntidadePorId(id, usuarioId);
        contaRepository.delete(conta);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoConsolidado(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .filter(c -> c.getTipoConta() != null && !c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoInvestimentos(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .filter(c -> c.getTipoConta() != null && c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularPatrimonioTotal(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoSaldosDto obterResumoSaldos(Long usuarioId) {
        List<Conta> contasAtivas = contaRepository.findByUsuarioId(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .toList();

        BigDecimal saldoCorrentes = contasAtivas.stream()
                .filter(c -> c.getTipoConta() != null && !c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoInvestimentos = contasAtivas.stream()
                .filter(c -> c.getTipoConta() != null && c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal patrimonioTotal = saldoCorrentes.add(saldoInvestimentos);

        long totalCorrentes = contasAtivas.stream()
                .filter(c -> c.getTipoConta() != null && !c.getTipoConta().isInvestimentoOuReserva())
                .count();

        long totalInvestimentos = contasAtivas.stream()
                .filter(c -> c.getTipoConta() != null && c.getTipoConta().isInvestimentoOuReserva())
                .count();

        return new ResumoSaldosDto(saldoCorrentes, saldoInvestimentos, patrimonioTotal, totalCorrentes, totalInvestimentos);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoContasResponseDto obterResumoContas(Long usuarioId) {
        List<Conta> todas = contaRepository.findByUsuarioId(usuarioId);

        List<ContaResponseDto> correntes = todas.stream()
                .filter(c -> c.getTipoConta() != null && !c.getTipoConta().isInvestimentoOuReserva())
                .map(DtoMapper::toContaResponse)
                .toList();

        List<ContaResponseDto> investimentos = todas.stream()
                .filter(c -> c.getTipoConta() != null && c.getTipoConta().isInvestimentoOuReserva())
                .map(DtoMapper::toContaResponse)
                .toList();

        BigDecimal subtotalCorrentes = correntes.stream()
                .map(c -> c.saldo() != null ? c.saldo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal subtotalInvestimentos = investimentos.stream()
                .map(c -> c.saldo() != null ? c.saldo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ResumoSaldosDto resumoSaldos = obterResumoSaldos(usuarioId);

        return new ResumoContasResponseDto(correntes, investimentos, subtotalCorrentes, subtotalInvestimentos, resumoSaldos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransacaoResponseDto> obterExtratoDetalhado(Long contaId, Long usuarioId) {
        Conta conta = buscarEntidadePorId(contaId, usuarioId);
        List<Transacao> extrato = transacaoRepository.findByContaIdAndUsuarioId(conta.getId(), usuarioId);
        return extrato.stream()
                .map(DtoMapper::toTransacaoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Conta salvarEntidade(Conta conta) {
        return contaRepository.save(conta);
    }
}
