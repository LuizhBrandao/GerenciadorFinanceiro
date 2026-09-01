package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.TransacaoRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.repository.spec.TransacaoSpecs;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransacaoServiceImpl implements TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ContaService contaService;
    private final CategoriaService categoriaService;

    @Override
    @Transactional(readOnly = true)
    public List<TransacaoResponseDto> listarTransacoes(Long usuarioId) {
        return transacaoRepository.findByUsuarioId(usuarioId).stream()
                .map(DtoMapper::toTransacaoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransacaoResponseDto> buscarAvancado(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal,
                                                     Long categoriaId, Long contaId, BigDecimal valorMin, BigDecimal valorMax) {
        Specification<Transacao> spec = Specification.where(TransacaoSpecs.porUsuario(usuarioId));

        if (dataInicial != null) {
            spec = spec.and(TransacaoSpecs.dataMaiorOuIgual(dataInicial));
        }
        if (dataFinal != null) {
            spec = spec.and(TransacaoSpecs.dataMenorOuIgual(dataFinal));
        }
        if (categoriaId != null) {
            spec = spec.and(TransacaoSpecs.porCategoria(categoriaId));
        }
        if (contaId != null) {
            spec = spec.and(TransacaoSpecs.porConta(contaId));
        }
        if (valorMin != null) {
            spec = spec.and(TransacaoSpecs.valorMaiorOuIgual(valorMin));
        }
        if (valorMax != null) {
            spec = spec.and(TransacaoSpecs.valorMenorOuIgual(valorMax));
        }

        return transacaoRepository.findAll(spec).stream()
                .map(DtoMapper::toTransacaoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransacaoResponseDto buscarPorId(Long id, Long usuarioId) {
        return DtoMapper.toTransacaoResponse(buscarEntidadePorId(id, usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public Transacao buscarEntidadePorId(Long id, Long usuarioId) {
        return transacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Transação não encontrada ou não pertence ao usuário."));
    }

    @Override
    @Transactional
    public TransacaoResponseDto salvar(TransacaoRequestDto request, Usuario usuario) {
        Conta conta = contaService.buscarEntidadePorId(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuario.getId());

        StatusTransacao status = request.status() != null ? request.status() : StatusTransacao.PAGA;

        Transacao transacao = new Transacao(
                usuario,
                request.descricao(),
                request.valor(),
                request.tipo(),
                status,
                request.dataTransacao(),
                conta,
                categoria,
                request.observacao()
        );

        if (transacao.getStatus() == StatusTransacao.PAGA) {
            atualizarSaldoConta(conta, transacao.getTipo(), transacao.getValor(), false);
        }

        Transacao salva = transacaoRepository.save(transacao);
        return DtoMapper.toTransacaoResponse(salva);
    }

    @Override
    @Transactional
    public Transacao salvarEntidade(Transacao transacao) {
        Conta conta = transacao.getConta();
        if (conta != null && transacao.getStatus() == StatusTransacao.PAGA) {
            atualizarSaldoConta(conta, transacao.getTipo(), transacao.getValor(), false);
        }
        return transacaoRepository.save(transacao);
    }

    @Override
    @Transactional
    public TransacaoResponseDto atualizar(Long id, TransacaoRequestDto request, Long usuarioId) {
        Transacao existente = buscarEntidadePorId(id, usuarioId);

        Conta contaAntiga = existente.getConta();
        TipoTransacao tipoAntigo = existente.getTipo();
        BigDecimal valorAntigo = existente.getValor();
        StatusTransacao statusAntigo = existente.getStatus();

        // Se estava paga, estorna da conta antiga
        if (statusAntigo == StatusTransacao.PAGA && contaAntiga != null) {
            atualizarSaldoConta(contaAntiga, tipoAntigo, valorAntigo, true);
        }

        Conta novaConta = contaService.buscarEntidadePorId(request.contaId(), usuarioId);
        Categoria novaCategoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuarioId);
        StatusTransacao novoStatus = request.status() != null ? request.status() : StatusTransacao.PAGA;

        existente.setDescricao(request.descricao());
        existente.setValor(request.valor());
        existente.setTipo(request.tipo());
        existente.setStatus(novoStatus);
        existente.setDataTransacao(request.dataTransacao());
        existente.setConta(novaConta);
        existente.setCategoria(novaCategoria);
        existente.setObservacao(request.observacao());

        // Se o novo status é PAGA, debita/credita na nova conta
        if (novoStatus == StatusTransacao.PAGA) {
            atualizarSaldoConta(novaConta, existente.getTipo(), existente.getValor(), false);
        }

        Transacao salva = transacaoRepository.save(existente);
        return DtoMapper.toTransacaoResponse(salva);
    }

    @Override
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        Transacao transacao = buscarEntidadePorId(id, usuarioId);

        if (transacao.getStatus() == StatusTransacao.PAGA && transacao.getConta() != null) {
            atualizarSaldoConta(transacao.getConta(), transacao.getTipo(), transacao.getValor(), true);
        }

        transacaoRepository.delete(transacao);
    }

    @Override
    @Transactional
    public List<TransacaoResponseDto> criarParcelamento(TransacaoRequestDto request, int quantidadeParcelas, Usuario usuario) {
        if (quantidadeParcelas <= 0) {
            throw new IllegalArgumentException("A quantidade de parcelas deve ser de no mínimo 1.");
        }

        Conta conta = contaService.buscarEntidadePorId(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuario.getId());

        BigDecimal valorTotal = request.valor();
        BigDecimal valorParcela = valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);

        List<Transacao> parcelas = new ArrayList<>();

        for (int i = 0; i < quantidadeParcelas; i++) {
            String descricaoParcela = request.descricao() + " (Parcela " + (i + 1) + "/" + quantidadeParcelas + ")";
            StatusTransacao status = (i == 0 && request.status() != null) ? request.status() : StatusTransacao.PENDENTE;
            LocalDate dataParcela = request.dataTransacao().plusMonths(i);

            Transacao parcela = new Transacao(
                    usuario,
                    descricaoParcela,
                    valorParcela,
                    request.tipo(),
                    status,
                    dataParcela,
                    conta,
                    categoria,
                    request.observacao()
            );

            if (status == StatusTransacao.PAGA) {
                atualizarSaldoConta(conta, parcela.getTipo(), parcela.getValor(), false);
            }

            parcelas.add(transacaoRepository.save(parcela));
        }

        return parcelas.stream()
                .map(DtoMapper::toTransacaoResponse)
                .collect(Collectors.toList());
    }

    private void atualizarSaldoConta(Conta conta, TipoTransacao tipo, BigDecimal valor, boolean isEstorno) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if ((tipo == TipoTransacao.RECEITA && !isEstorno) || (tipo == TipoTransacao.DESPESA && isEstorno)) {
            conta.creditar(valor);
        } else if ((tipo == TipoTransacao.DESPESA && !isEstorno) || (tipo == TipoTransacao.RECEITA && isEstorno)) {
            conta.debitar(valor);
        }
        contaService.salvarEntidade(conta);
    }
}
