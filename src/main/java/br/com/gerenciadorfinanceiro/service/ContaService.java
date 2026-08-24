package br.com.gerenciadorfinanceiro.service;

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

    public Conta salvar(Conta conta) {
        return contaRepository.save(conta);
    }

    public void excluir(Long id, Long usuarioId) {
        Conta conta = buscarPorId(id, usuarioId);
        contaRepository.delete(conta);
    }

    public BigDecimal calcularSaldoConsolidado(Long usuarioId) {
        return listarContas(usuarioId).stream()
                .map(Conta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transacao> obterExtratoDetalhado(Long contaId, Long usuarioId) {
        Conta conta = buscarPorId(contaId, usuarioId);
        return transacaoRepository.findByContaIdAndUsuarioId(conta.getId(), usuarioId);
    }
}
