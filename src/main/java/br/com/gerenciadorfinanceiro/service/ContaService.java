package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.repository.ContaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContaService {

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    public List<Conta> listarContas(Long usuarioId) {
        return contaRepository.findByUsuarioId(usuarioId);
    }

    public Conta buscarPorId(Long id, Long usuarioId) {
        return contaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Conta não encontrada ou não pertence ao usuário."));
    }

    @SuppressWarnings("null")
    public Conta salvar(Conta conta) {
        return contaRepository.save(conta);
    }

    @SuppressWarnings("null")
    public void excluir(Long id, Long usuarioId) {
        Conta conta = buscarPorId(id, usuarioId);
        contaRepository.delete(conta);
    }

    @SuppressWarnings("null")
    public BigDecimal calcularSaldoConsolidado(Long usuarioId) {
        // Saldo consolidado refere-se às contas correntes e de giro (excluindo investimentos e poupança)
        return listarContas(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .filter(c -> c.getTipoConta() != null && !c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("null")
    public BigDecimal calcularSaldoInvestimentos(Long usuarioId) {
        // Total alocado em investimentos e poupança / reserva
        return listarContas(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .filter(c -> c.getTipoConta() != null && c.getTipoConta().isInvestimentoOuReserva())
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("null")
    public BigDecimal calcularPatrimonioTotal(Long usuarioId) {
        // Patrimônio líquido total somando contas correntes + investimentos e poupança
        return listarContas(usuarioId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("null")
    public ResumoSaldosDto obterResumoSaldos(Long usuarioId) {
        List<Conta> contasAtivas = listarContas(usuarioId).stream()
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

    public List<Transacao> obterExtratoDetalhado(Long contaId, Long usuarioId) {
        Conta conta = buscarPorId(contaId, usuarioId);
        return transacaoRepository.findByContaIdAndUsuarioId(conta.getId(), usuarioId);
    }
}
