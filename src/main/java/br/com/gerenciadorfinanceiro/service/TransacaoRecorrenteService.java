package br.com.gerenciadorfinanceiro.service;

import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.repository.CategoriaRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRecorrenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransacaoRecorrenteService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoRecorrenteService.class);

    @Autowired
    private TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private ContaService contaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<TransacaoRecorrente> listar(Long usuarioId) {
        return transacaoRecorrenteRepository.findByUsuarioId(usuarioId);
    }

    public List<TransacaoRecorrente> listarAtivas(Long usuarioId) {
        return transacaoRecorrenteRepository.findByUsuarioIdAndAtivoTrue(usuarioId);
    }

    public TransacaoRecorrente buscarPorId(Long id, Long usuarioId) {
        return transacaoRecorrenteRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Transação recorrente não encontrada ou não pertence ao usuário."));
    }

    @Transactional
    public TransacaoRecorrente salvar(TransacaoRecorrente recorrencia) {
        // Valida conta e categoria
        Conta conta = contaService.buscarPorId(recorrencia.getConta().getId(), recorrencia.getUsuario().getId());
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(recorrencia.getCategoria().getId(), recorrencia.getUsuario().getId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Categoria não encontrada ou não pertence ao usuário."));

        recorrencia.setConta(conta);
        recorrencia.setCategoria(categoria);

        if (recorrencia.getDiaVencimento() == null || recorrencia.getDiaVencimento() < 1) {
            recorrencia.setDiaVencimento(1);
        } else if (recorrencia.getDiaVencimento() > 31) {
            recorrencia.setDiaVencimento(31);
        }

        return transacaoRecorrenteRepository.save(recorrencia);
    }

    @Transactional
    public TransacaoRecorrente alternarStatus(Long id, Long usuarioId) {
        TransacaoRecorrente recorrencia = buscarPorId(id, usuarioId);
        recorrencia.setAtivo(!recorrencia.getAtivo());
        return transacaoRecorrenteRepository.save(recorrencia);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        TransacaoRecorrente recorrencia = buscarPorId(id, usuarioId);
        transacaoRecorrenteRepository.delete(recorrencia);
    }

    @Transactional
    public Transacao lancarInstancia(Long id, Long usuarioId, LocalDate dataLancamento) {
        TransacaoRecorrente recorrencia = buscarPorId(id, usuarioId);
        return gerarTransacao(recorrencia, dataLancamento != null ? dataLancamento : calcularDataAlvo(recorrencia, LocalDate.now()));
    }

    @Transactional
    public List<Transacao> processarRecorrenciasUsuario(Long usuarioId, LocalDate dataReferencia) {
        LocalDate ref = (dataReferencia != null) ? dataReferencia : LocalDate.now();
        List<TransacaoRecorrente> ativas = transacaoRecorrenteRepository.findByUsuarioIdAndAtivoTrue(usuarioId);
        List<Transacao> geradas = new ArrayList<>();

        for (TransacaoRecorrente rec : ativas) {
            if (deveGerarInstancia(rec, ref)) {
                LocalDate dataAlvo = calcularDataAlvo(rec, ref);
                Transacao t = gerarTransacao(rec, dataAlvo);
                geradas.add(t);
            }
        }
        return geradas;
    }

    /**
     * Job agendado que roda diariamente às 01:00 da manhã para gerar lançamentos automáticos.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processarTodasRecorrenciasAutomaticamente() {
        log.info("Iniciando rotina diária de processamento de transações recorrentes...");
        LocalDate hoje = LocalDate.now();
        List<TransacaoRecorrente> ativas = transacaoRecorrenteRepository.findByAtivoTrue();
        int count = 0;

        for (TransacaoRecorrente rec : ativas) {
            try {
                if (deveGerarInstancia(rec, hoje)) {
                    LocalDate dataAlvo = calcularDataAlvo(rec, hoje);
                    gerarTransacao(rec, dataAlvo);
                    count++;
                }
            } catch (Exception e) {
                log.error("Erro ao processar transação recorrente ID: {}", rec.getId(), e);
            }
        }
        log.info("Processamento concluído. {} transações geradas automaticamente.", count);
    }

    private boolean deveGerarInstancia(TransacaoRecorrente rec, LocalDate ref) {
        if (!Boolean.TRUE.equals(rec.getAtivo())) {
            return false;
        }

        // Verifica período de vigência
        if (rec.getDataInicio() != null && ref.isBefore(rec.getDataInicio())) {
            return false;
        }
        if (rec.getDataFim() != null && ref.isAfter(rec.getDataFim())) {
            return false;
        }

        FrequenciaRecorrencia freq = rec.getFrequencia() != null ? rec.getFrequencia() : FrequenciaRecorrencia.MENSAL;
        LocalDate ultimo = rec.getUltimoLancamento();

        switch (freq) {
            case DIARIA:
                return ultimo == null || ultimo.isBefore(ref);

            case SEMANAL:
                return ultimo == null || ChronoUnit.DAYS.between(ultimo, ref) >= 7;

            case QUINZENAL:
                return ultimo == null || ChronoUnit.DAYS.between(ultimo, ref) >= 15;

            case MENSAL:
                if (ultimo != null && ultimo.getYear() == ref.getYear() && ultimo.getMonthValue() == ref.getMonthValue()) {
                    return false; // Já lançado neste mês
                }
                LocalDate dataAlvoMensal = calcularDataAlvo(rec, ref);
                return !ref.isBefore(dataAlvoMensal);

            case ANUAL:
                if (ultimo != null && ultimo.getYear() == ref.getYear()) {
                    return false; // Já lançado neste ano
                }
                LocalDate dataAlvoAnual = calcularDataAlvo(rec, ref);
                return !ref.isBefore(dataAlvoAnual);

            default:
                return false;
        }
    }

    private LocalDate calcularDataAlvo(TransacaoRecorrente rec, LocalDate ref) {
        int maxDia = ref.lengthOfMonth();
        int dia = Math.min(rec.getDiaVencimento() != null ? rec.getDiaVencimento() : 1, maxDia);
        return LocalDate.of(ref.getYear(), ref.getMonthValue(), dia);
    }

    private Transacao gerarTransacao(TransacaoRecorrente rec, LocalDate dataTransacao) {
        String sufixoObservacao = rec.getObservacao() != null && !rec.getObservacao().isBlank()
                ? " - " + rec.getObservacao()
                : "";

        Transacao transacao = new Transacao(
                rec.getUsuario(),
                rec.getDescricao() + " (Recorrência)",
                rec.getValor(),
                rec.getTipo(),
                StatusTransacao.PENDENTE,
                dataTransacao,
                rec.getConta(),
                rec.getCategoria(),
                "[Lançamento Automático Recorrente]" + sufixoObservacao
        );

        Transacao salva = transacaoService.salvar(transacao);

        rec.setUltimoLancamento(dataTransacao);
        transacaoRecorrenteRepository.save(rec);

        log.info("Transação recorrente lançada com sucesso: {} (R$ {}) para o dia {}",
                rec.getDescricao(), rec.getValor(), dataTransacao);

        return salva;
    }
}
