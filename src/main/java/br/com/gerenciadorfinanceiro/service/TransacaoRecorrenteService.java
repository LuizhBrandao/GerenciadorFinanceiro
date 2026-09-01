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
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransacaoRecorrenteService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoRecorrenteService.class);

    @Autowired
    private TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

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

        if (recorrencia.getDataInicio() == null) {
            recorrencia.setDataInicio(LocalDate.now());
        }

        TransacaoRecorrente salva = transacaoRecorrenteRepository.save(recorrencia);

        // Gera automaticamente todas as ocorrências retroativas (passadas) e futuras para o ano
        gerarOcorrenciasRecorrencia(salva, null);

        return salva;
    }

    @Transactional
    public TransacaoRecorrente atualizar(Long id, TransacaoRecorrente dados, Long usuarioId) {
        TransacaoRecorrente existente = buscarPorId(id, usuarioId);

        Conta conta = contaService.buscarPorId(dados.getConta().getId(), usuarioId);
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(dados.getCategoria().getId(), usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Categoria não encontrada ou não pertence ao usuário."));

        String descricaoAntiga = existente.getDescricao();

        existente.setDescricao(dados.getDescricao());
        existente.setValor(dados.getValor());
        existente.setTipo(dados.getTipo());
        existente.setFrequencia(dados.getFrequencia());
        existente.setDiaVencimento(dados.getDiaVencimento());
        existente.setConta(conta);
        existente.setCategoria(categoria);
        existente.setDataInicio(dados.getDataInicio());
        existente.setDataFim(dados.getDataFim());
        existente.setObservacao(dados.getObservacao());

        TransacaoRecorrente salva = transacaoRecorrenteRepository.save(existente);

        // Atualiza em cascata a categoria, conta e descrição de todas as transações desta recorrência
        sincronizarTransacoesDaRecorrencia(salva, descricaoAntiga);

        // Gera eventuais novos meses se a vigência mudou
        gerarOcorrenciasRecorrencia(salva, null);

        return salva;
    }

    @Transactional
    public void sincronizarTransacoesDaRecorrencia(TransacaoRecorrente rec, String descricaoAntiga) {
        List<Transacao> transacoes = transacaoRepository.findByUsuarioId(rec.getUsuario().getId());

        for (Transacao t : transacoes) {
            boolean pertence = (t.getRecorrencia() != null && t.getRecorrencia().getId().equals(rec.getId()))
                    || (t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()))
                    || (descricaoAntiga != null && t.getDescricao().trim().equalsIgnoreCase(descricaoAntiga.trim()));

            if (pertence) {
                t.setRecorrencia(rec);
                t.setDescricao(rec.getDescricao());
                t.setCategoria(rec.getCategoria());
                t.setConta(rec.getConta());
                t.setTipo(rec.getTipo());

                // Se a transação estiver pendente, atualiza também o valor caso tenha mudado
                if (t.getStatus() == StatusTransacao.PENDENTE && rec.getValor() != null) {
                    t.setValor(rec.getValor());
                }

                transacaoRepository.save(t);
            }
        }
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
        LocalDate dataAlvo = dataLancamento != null ? dataLancamento : calcularDataAlvo(recorrencia, LocalDate.now());
        return criarEGravarTransacao(recorrencia, dataAlvo, StatusTransacao.PAGA);
    }

    @Transactional
    public List<Transacao> processarRecorrenciasUsuario(Long usuarioId, LocalDate dataReferencia) {
        List<TransacaoRecorrente> ativas = transacaoRecorrenteRepository.findByUsuarioIdAndAtivoTrue(usuarioId);
        List<Transacao> todasGeradas = new ArrayList<>();

        for (TransacaoRecorrente rec : ativas) {
            sincronizarTransacoesDaRecorrencia(rec, null);
            List<Transacao> geradas = gerarOcorrenciasRecorrencia(rec, dataReferencia);
            todasGeradas.addAll(geradas);
        }
        return todasGeradas;
    }

    /**
     * Gera e sincroniza todas as instâncias da recorrência desde a data de início até a data limite (fim do ano ou dataFim).
     * - Meses passados recebem StatusTransacao.PAGA
     * - Mês atual / meses futuros recebem StatusTransacao.PENDENTE
     */
    @Transactional
    public List<Transacao> gerarOcorrenciasRecorrencia(TransacaoRecorrente rec, LocalDate ateData) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = rec.getDataInicio() != null ? rec.getDataInicio() : hoje;
        
        int anoLimite = Math.max(hoje.getYear(), inicio.getYear());
        LocalDate fim = rec.getDataFim() != null 
                ? rec.getDataFim() 
                : (ateData != null ? ateData : LocalDate.of(anoLimite, 12, 31));

        if (fim.isBefore(inicio)) {
            return List.of();
        }

        FrequenciaRecorrencia freq = rec.getFrequencia() != null ? rec.getFrequencia() : FrequenciaRecorrencia.MENSAL;
        List<Transacao> geradas = new ArrayList<>();
        LocalDate cursor = inicio;

        // Busca transações já cadastradas do usuário para prevenir duplicidades
        List<Transacao> existentes = transacaoRepository.findByUsuarioId(rec.getUsuario().getId());

        if (freq == FrequenciaRecorrencia.MENSAL) {
            LocalDate mesAtual = LocalDate.of(inicio.getYear(), inicio.getMonth(), 1);
            LocalDate mesFim = LocalDate.of(fim.getYear(), fim.getMonth(), 1);

            while (!mesAtual.isAfter(mesFim)) {
                int maxDia = mesAtual.lengthOfMonth();
                int dia = Math.min(rec.getDiaVencimento() != null ? rec.getDiaVencimento() : 1, maxDia);
                LocalDate dataAlvo = LocalDate.of(mesAtual.getYear(), mesAtual.getMonth(), dia);

                if (!dataAlvo.isBefore(inicio) && !dataAlvo.isAfter(fim)) {
                    // Verifica se já existe lançamento similar no mesmo mês/ano
                    boolean jaExiste = existentes.stream().anyMatch(t ->
                            t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()) &&
                            t.getDataTransacao().getYear() == dataAlvo.getYear() &&
                            t.getDataTransacao().getMonth() == dataAlvo.getMonth()
                    );

                    if (!jaExiste) {
                        StatusTransacao status = dataAlvo.isBefore(hoje) ? StatusTransacao.PAGA : StatusTransacao.PENDENTE;
                        Transacao t = criarEGravarTransacao(rec, dataAlvo, status);
                        geradas.add(t);
                        existentes.add(t);
                    }
                }
                mesAtual = mesAtual.plusMonths(1);
            }
        } else if (freq == FrequenciaRecorrencia.SEMANAL) {
            while (!cursor.isAfter(fim)) {
                final LocalDate dataAlvo = cursor;
                boolean jaExiste = existentes.stream().anyMatch(t ->
                        t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()) &&
                        t.getDataTransacao().equals(dataAlvo)
                );
                if (!jaExiste) {
                    StatusTransacao status = dataAlvo.isBefore(hoje) ? StatusTransacao.PAGA : StatusTransacao.PENDENTE;
                    Transacao t = criarEGravarTransacao(rec, dataAlvo, status);
                    geradas.add(t);
                    existentes.add(t);
                }
                cursor = cursor.plusWeeks(1);
            }
        } else if (freq == FrequenciaRecorrencia.QUINZENAL) {
            while (!cursor.isAfter(fim)) {
                final LocalDate dataAlvo = cursor;
                boolean jaExiste = existentes.stream().anyMatch(t ->
                        t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()) &&
                        t.getDataTransacao().equals(dataAlvo)
                );
                if (!jaExiste) {
                    StatusTransacao status = dataAlvo.isBefore(hoje) ? StatusTransacao.PAGA : StatusTransacao.PENDENTE;
                    Transacao t = criarEGravarTransacao(rec, dataAlvo, status);
                    geradas.add(t);
                    existentes.add(t);
                }
                cursor = cursor.plusDays(15);
            }
        } else if (freq == FrequenciaRecorrencia.ANUAL) {
            LocalDate anoCursor = inicio;
            while (!anoCursor.isAfter(fim)) {
                final LocalDate dataAlvo = anoCursor;
                boolean jaExiste = existentes.stream().anyMatch(t ->
                        t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()) &&
                        t.getDataTransacao().getYear() == dataAlvo.getYear()
                );
                if (!jaExiste) {
                    StatusTransacao status = dataAlvo.isBefore(hoje) ? StatusTransacao.PAGA : StatusTransacao.PENDENTE;
                    Transacao t = criarEGravarTransacao(rec, dataAlvo, status);
                    geradas.add(t);
                    existentes.add(t);
                }
                anoCursor = anoCursor.plusYears(1);
            }
        } else if (freq == FrequenciaRecorrencia.DIARIA) {
            while (!cursor.isAfter(fim)) {
                final LocalDate dataAlvo = cursor;
                boolean jaExiste = existentes.stream().anyMatch(t ->
                        t.getDescricao().trim().equalsIgnoreCase(rec.getDescricao().trim()) &&
                        t.getDataTransacao().equals(dataAlvo)
                );
                if (!jaExiste) {
                    StatusTransacao status = dataAlvo.isBefore(hoje) ? StatusTransacao.PAGA : StatusTransacao.PENDENTE;
                    Transacao t = criarEGravarTransacao(rec, dataAlvo, status);
                    geradas.add(t);
                    existentes.add(t);
                }
                cursor = cursor.plusDays(1);
            }
        }

        if (!geradas.isEmpty()) {
            rec.setUltimoLancamento(geradas.get(geradas.size() - 1).getDataTransacao());
            transacaoRecorrenteRepository.save(rec);
        }

        return geradas;
    }

    /**
     * Job agendado que roda diariamente às 01:00 da manhã para atualizar status de lançamentos ou gerar novas instâncias.
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
                List<Transacao> geradas = gerarOcorrenciasRecorrencia(rec, hoje);
                count += geradas.size();
            } catch (Exception e) {
                log.error("Erro ao processar transação recorrente ID: {}", rec.getId(), e);
            }
        }
        log.info("Processamento concluído. {} transações geradas automaticamente.", count);
    }

    private LocalDate calcularDataAlvo(TransacaoRecorrente rec, LocalDate ref) {
        int maxDia = ref.lengthOfMonth();
        int dia = Math.min(rec.getDiaVencimento() != null ? rec.getDiaVencimento() : 1, maxDia);
        return LocalDate.of(ref.getYear(), ref.getMonthValue(), dia);
    }

    private Transacao criarEGravarTransacao(TransacaoRecorrente rec, LocalDate dataTransacao, StatusTransacao status) {
        String sufixoObservacao = rec.getObservacao() != null && !rec.getObservacao().isBlank()
                ? " - " + rec.getObservacao()
                : "";

        Transacao transacao = new Transacao(
                rec.getUsuario(),
                rec.getDescricao(),
                rec.getValor(),
                rec.getTipo(),
                status,
                dataTransacao,
                rec.getConta(),
                rec.getCategoria(),
                "[Lançamento Recorrente]" + sufixoObservacao
        );

        return transacaoService.salvar(transacao);
    }
}
