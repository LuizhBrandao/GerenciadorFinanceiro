package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransacaoService {

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private ContaService contaService;

    public List<Transacao> listarTransacoes(Long usuarioId) {
        return transacaoRepository.findByUsuarioId(usuarioId);
    }

    public List<Transacao> buscarAvancado(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal, 
                                          Long categoriaId, Long contaId, BigDecimal valorMin, BigDecimal valorMax) {
        var spec = org.springframework.data.jpa.domain.Specification.where(
                br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.porUsuario(usuarioId));

        if (dataInicial != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.dataMaiorOuIgual(dataInicial));
        }
        if (dataFinal != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.dataMenorOuIgual(dataFinal));
        }
        if (categoriaId != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.porCategoria(categoriaId));
        }
        if (contaId != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.porConta(contaId));
        }
        if (valorMin != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.valorMaiorOuIgual(valorMin));
        }
        if (valorMax != null) {
            spec = spec.and(br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs.valorMenorOuIgual(valorMax));
        }

        return transacaoRepository.findAll(spec);
    }

    public Transacao buscarPorId(Long id, Long usuarioId) {
        return transacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Transação não encontrada ou não pertence ao usuário."));
    }

    @Transactional
    public Transacao salvar(Transacao transacao) {
        // Valida se a conta pertence ao usuário
        Conta conta = contaService.buscarPorId(transacao.getConta().getId(), transacao.getUsuario().getId());
        transacao.setConta(conta);

        if (transacao.getStatus() == StatusTransacao.PAGA) {
            atualizarSaldoConta(conta, transacao.getTipo(), transacao.getValor(), false);
        }

        return transacaoRepository.save(transacao);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Transacao transacao = buscarPorId(id, usuarioId);
        
        if (transacao.getStatus() == StatusTransacao.PAGA) {
            atualizarSaldoConta(transacao.getConta(), transacao.getTipo(), transacao.getValor(), true);
        }
        
        transacaoRepository.delete(transacao);
    }

    @Transactional
    public List<Transacao> criarParcelamento(Transacao transacaoBase, int quantidadeParcelas) {
        List<Transacao> parcelas = new ArrayList<>();
        BigDecimal valorParcela = transacaoBase.getValor().divide(new BigDecimal(quantidadeParcelas), 2, java.math.RoundingMode.HALF_UP);
        
        for (int i = 0; i < quantidadeParcelas; i++) {
            Transacao parcela = new Transacao(
                    transacaoBase.getUsuario(),
                    transacaoBase.getDescricao() + " (Parcela " + (i + 1) + "/" + quantidadeParcelas + ")",
                    valorParcela,
                    transacaoBase.getTipo(),
                    i == 0 ? transacaoBase.getStatus() : StatusTransacao.PENDENTE,
                    transacaoBase.getDataTransacao().plusMonths(i),
                    transacaoBase.getConta(),
                    transacaoBase.getCategoria(),
                    transacaoBase.getObservacao()
            );
            
            parcela.setTags(transacaoBase.getTags());
            parcelas.add(salvar(parcela));
        }
        return parcelas;
    }

    private void atualizarSaldoConta(Conta conta, TipoTransacao tipo, BigDecimal valor, boolean isEstorno) {
        if ((tipo == TipoTransacao.RECEITA && !isEstorno) || (tipo == TipoTransacao.DESPESA && isEstorno)) {
            conta.creditar(valor);
        } else if ((tipo == TipoTransacao.DESPESA && !isEstorno) || (tipo == TipoTransacao.RECEITA && isEstorno)) {
            conta.debitar(valor);
        }
        contaService.salvar(conta);
    }
}
