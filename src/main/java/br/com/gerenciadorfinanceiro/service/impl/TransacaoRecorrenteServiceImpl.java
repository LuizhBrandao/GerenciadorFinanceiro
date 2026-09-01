package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaRequestDto;
import br.com.gerenciadorfinanceiro.controller.dto.RecorrenciaResponseDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoRecorrenciasDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResponseDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.exception.EntidadeNaoEncontradaException;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Conta;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.TransacaoRecorrente;
import br.com.gerenciadorfinanceiro.model.Usuario;
import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRecorrenteRepository;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.CategoriaService;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.TransacaoRecorrenteService;
import br.com.gerenciadorfinanceiro.service.TransacaoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransacaoRecorrenteServiceImpl implements TransacaoRecorrenteService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoRecorrenteServiceImpl.class);

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final TransacaoRepository transacaoRepository;
    private final TransacaoService transacaoService;
    private final ContaService contaService;
    private final CategoriaService categoriaService;

    @Override
    @Transactional(readOnly = true)
    public List<RecorrenciaResponseDto> listar(Long usuarioId) {
        return transacaoRecorrenteRepository.findByUsuarioId(usuarioId).stream()
                .map(DtoMapper::toRecorrenciaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecorrenciaResponseDto> listarAtivas(Long usuarioId) {
        return transacaoRecorrenteRepository.findByUsuarioIdAndAtivoTrue(usuarioId).stream()
                .map(DtoMapper::toRecorrenciaResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecorrenciaResponseDto buscarPorId(Long id, Long usuarioId) {
        return DtoMapper.toRecorrenciaResponse(buscarEntidadePorId(id, usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public TransacaoRecorrente buscarEntidadePorId(Long id, Long usuarioId) {
        return transacaoRecorrenteRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Transação recorrente não encontrada ou não pertence ao usuário."));
    }

    @Override
    @Transactional
    public RecorrenciaResponseDto salvar(RecorrenciaRequestDto request, Usuario usuario) {
        Conta conta = contaService.buscarEntidadePorId(request.contaId(), usuario.getId());
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuario.getId());

        int diaVencimento = (request.diaVencimento() != null) ? request.diaVencimento() : 1;
        if (diaVencimento < 1) diaVencimento = 1;
        if (diaVencimento > 31) diaVencimento = 31;

        LocalDate dataInicio = (request.dataInicio() != null) ? request.dataInicio() : LocalDate.now();
        FrequenciaRecorrencia freq = (request.frequencia() != null) ? request.frequencia() : FrequenciaRecorrencia.MENSAL;

        TransacaoRecorrente recorrencia = new TransacaoRecorrente(
                usuario,
                request.descricao(),
                request.valor(),
                request.tipo(),
                freq,
                diaVencimento,
                conta,
                categoria,
                dataInicio,
                request.dataFim(),
                request.observacao()
        );

        TransacaoRecorrente salva = transacaoRecorrenteRepository.save(recorrencia);
        gerarOcorrenciasRecorrencia(salva, null);

        return DtoMapper.toRecorrenciaResponse(salva);
    }

    @Override
    @Transactional
    public RecorrenciaResponseDto atualizar(Long id, RecorrenciaRequestDto request, Long usuarioId) {
        TransacaoRecorrente existente = buscarEntidadePorId(id, usuarioId);

        Conta conta = contaService.buscarEntidadePorId(request.contaId(), usuarioId);
        Categoria categoria = categoriaService.buscarEntidadePorId(request.categoriaId(), usuarioId);

        String descricaoAntiga = existente.getDescricao();

        int diaVencimento = (request.diaVencimento() != null) ? request.diaVencimento() : 1;
        if (diaVencimento < 1) diaVencimento = 1;
        if (diaVencimento > 31) diaVencimento = 31;

        FrequenciaRecorrencia freq = (request.frequencia() != null) ? request.frequencia() : FrequenciaRecorrencia.MENSAL;

        existente.setDescricao(request.descricao());
        existente.setValor(request.valor());
        existente.setTipo(request.tipo());
        existente.setFrequencia(freq);
        existente.setDiaVencimento(diaVencimento);
        existente.setConta(conta);
        existente.setCategoria(categoria);
        existente.setDataInicio(request.dataInicio());
        existente.setDataFim(request.dataFim());
        existente.setObservacao(request.observacao());

        TransacaoRecorrente salva = transacaoRecorrenteRepository.save(existente);

        sincronizarTransacoesDaRecorrencia(salva, descricaoAntiga);
        gerarOcorrenciasRecorrencia(salva, null);

        return DtoMapper.toRecorrenciaResponse(salva);
    }

    @Override
    @Transactional
    public RecorrenciaResponseDto alternarStatus(Long id, Long usuarioId) {
        TransacaoRecorrente recorrencia = buscarEntidadePorId(id, usuarioId);
        recorrencia.setAtivo(!recorrencia.getAtivo());
        TransacaoRecorrente salva = transacaoRecorrenteRepository.save(recorrencia);
        return DtoMapper.toRecorrenciaResponse(salva);
    }

    @Override
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        TransacaoRecorrente recorrencia = buscarEntidadePorId(id, usuarioId);
        transacaoRecorrenteRepository.delete(recorrencia);
    }

    @Override
    @Transactional
    public TransacaoResponseDto lancarInstancia(Long id, Long usuarioId, LocalDate dataLancamento) {
        TransacaoRecorrente recorrencia = buscarEntidadePorId(id, usuarioId);
        LocalDate dataAlvo = dataLancamento != null ? dataLancamento : calcularDataAlvo(recorrencia, LocalDate.now());
        Transacao t = criarEGravarTransacao(recorrencia, dataAlvo, StatusTransacao.PAGA);
        return DtoMapper.toTransacaoResponse(t);
    }

    @Override
    @Transactional
    public List<TransacaoResponseDto> processarRecorrenciasUsuario(Long usuarioId, LocalDate dataReferencia) {
        List<TransacaoRecorrente> ativas = transacaoRecorrenteRepository.findByUsuarioIdAndAtivoTrue(usuarioId);
        List<Transacao> todasGeradas = new ArrayList<>();

        for (TransacaoRecorrente rec : ativas) {
            sincronizarTransacoesDaRecorrencia(rec, null);
            List<Transacao> geradas = gerarOcorrenciasRecorrencia(rec, dataReferencia);
            todasGeradas.addAll(geradas);
        }
        return todasGeradas.stream()
                .map(DtoMapper::toTransacaoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoRecorrenciasDto obterResumo(Long usuarioId) {
        List<TransacaoRecorrente> todas = transacaoRecorrenteRepository.findByUsuarioId(usuarioId);

        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal totalReceitas = BigDecimal.ZERO;
        long totalAtivas = 0;

        for (TransacaoRecorrente r : todas) {
            if (Boolean.TRUE.equals(r.getAtivo())) {
                totalAtivas++;
                BigDecimal val = r.getValor() != null ? r.getValor() : BigDecimal.ZERO;
                if (r.getTipo() == TipoTransacao.DESPESA) {
                    totalDespesas = totalDespesas.add(val);
                } else if (r.getTipo() == TipoTransacao.RECEITA) {
                    totalReceitas = totalReceitas.add(val);
                }
            }
        }

        return new ResumoRecorrenciasDto(totalDespesas, totalReceitas, totalAtivas);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    @Override
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

    private void sincronizarTransacoesDaRecorrencia(TransacaoRecorrente rec, String descricaoAntiga) {
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

                if (t.getStatus() == StatusTransacao.PENDENTE && rec.getValor() != null) {
                    t.setValor(rec.getValor());
                }

                transacaoRepository.save(t);
            }
        }
    }

    private List<Transacao> gerarOcorrenciasRecorrencia(TransacaoRecorrente rec, LocalDate ateData) {
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

        List<Transacao> existentes = transacaoRepository.findByUsuarioId(rec.getUsuario().getId());

        if (freq == FrequenciaRecorrencia.MENSAL) {
            LocalDate mesAtual = LocalDate.of(inicio.getYear(), inicio.getMonth(), 1);
            LocalDate mesFim = LocalDate.of(fim.getYear(), fim.getMonth(), 1);

            while (!mesAtual.isAfter(mesFim)) {
                int maxDia = mesAtual.lengthOfMonth();
                int dia = Math.min(rec.getDiaVencimento() != null ? rec.getDiaVencimento() : 1, maxDia);
                LocalDate dataAlvo = LocalDate.of(mesAtual.getYear(), mesAtual.getMonth(), dia);

                if (!dataAlvo.isBefore(inicio) && !dataAlvo.isAfter(fim)) {
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

        return transacaoService.salvarEntidade(transacao);
    }
}
