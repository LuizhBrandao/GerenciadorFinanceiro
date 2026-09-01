package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.DashboardDto;
import br.com.gerenciadorfinanceiro.controller.dto.DespesaPorCategoriaDto;
import br.com.gerenciadorfinanceiro.controller.dto.EvolucaoMensalDto;
import br.com.gerenciadorfinanceiro.controller.dto.MetricasMesDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResumoDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
import br.com.gerenciadorfinanceiro.model.Categoria;
import br.com.gerenciadorfinanceiro.model.Transacao;
import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.repository.TransacaoRepository;
import br.com.gerenciadorfinanceiro.service.ContaService;
import br.com.gerenciadorfinanceiro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final ContaService contaService;
    private final TransacaoRepository transacaoRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto obterDashboard(Long usuarioId) {
        ResumoSaldosDto saldos = contaService.obterResumoSaldos(usuarioId);
        List<Transacao> todasTransacoes = transacaoRepository.findByUsuarioId(usuarioId);

        LocalDate hoje = LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();

        MetricasMesDto metricasMes = calcularMetricasMes(todasTransacoes, mesAtual, anoAtual);
        List<TransacaoResumoDto> transacoesRecentes = extrairTransacoesRecentes(todasTransacoes, 5);
        List<DespesaPorCategoriaDto> despesasPorCategoria = calcularDespesasPorCategoria(todasTransacoes);
        EvolucaoMensalDto evolucaoMensal = calcularEvolucaoMensal(todasTransacoes, hoje, 6);

        return new DashboardDto(saldos, metricasMes, transacoesRecentes, despesasPorCategoria, evolucaoMensal);
    }

    private MetricasMesDto calcularMetricasMes(List<Transacao> transacoes, int mes, int ano) {
        BigDecimal receitasMes = BigDecimal.ZERO;
        BigDecimal despesasMes = BigDecimal.ZERO;
        BigDecimal receitasPendentes = BigDecimal.ZERO;
        BigDecimal despesasPendentes = BigDecimal.ZERO;

        for (Transacao t : transacoes) {
            LocalDate data = t.getDataTransacao();
            if (data != null && data.getMonthValue() == mes && data.getYear() == ano) {
                BigDecimal valor = t.getValor() != null ? t.getValor() : BigDecimal.ZERO;

                if (t.getTipo() == TipoTransacao.RECEITA) {
                    if (t.getStatus() == StatusTransacao.PAGA) {
                        receitasMes = receitasMes.add(valor);
                    } else if (t.getStatus() == StatusTransacao.PENDENTE) {
                        receitasPendentes = receitasPendentes.add(valor);
                    }
                } else if (t.getTipo() == TipoTransacao.DESPESA) {
                    if (t.getStatus() == StatusTransacao.PAGA) {
                        despesasMes = despesasMes.add(valor);
                    } else if (t.getStatus() == StatusTransacao.PENDENTE) {
                        despesasPendentes = despesasPendentes.add(valor);
                    }
                }
            }
        }

        BigDecimal balancoMes = receitasMes.subtract(despesasMes);
        return new MetricasMesDto(receitasMes, despesasMes, balancoMes, receitasPendentes, despesasPendentes);
    }

    private List<TransacaoResumoDto> extrairTransacoesRecentes(List<Transacao> transacoes, int limit) {
        return transacoes.stream()
                .sorted(Comparator.comparing(Transacao::getDataTransacao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(limit)
                .map(DtoMapper::toTransacaoResumo)
                .collect(Collectors.toList());
    }

    private List<DespesaPorCategoriaDto> calcularDespesasPorCategoria(List<Transacao> transacoes) {
        List<Transacao> despesasPagas = transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA && t.getStatus() == StatusTransacao.PAGA)
                .toList();

        BigDecimal totalGeral = despesasPagas.stream()
                .map(t -> t.getValor() != null ? t.getValor() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Categoria, BigDecimal> porCategoria = despesasPagas.stream()
                .collect(Collectors.groupingBy(
                        Transacao::getCategoria,
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getValor() != null ? t.getValor() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));

        return porCategoria.entrySet().stream()
                .map(entry -> {
                    Categoria cat = entry.getKey();
                    BigDecimal totalCat = entry.getValue();
                    BigDecimal percentual = (totalGeral.compareTo(BigDecimal.ZERO) > 0)
                            ? totalCat.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    Long catId = cat != null ? cat.getId() : null;
                    String nome = cat != null ? cat.getNome() : "Outras";
                    String icone = cat != null ? cat.getIcone() : "fa-tag";

                    return new DespesaPorCategoriaDto(catId, nome, icone, totalCat, percentual);
                })
                .sorted(Comparator.comparing(DespesaPorCategoriaDto::total).reversed())
                .collect(Collectors.toList());
    }

    private EvolucaoMensalDto calcularEvolucaoMensal(List<Transacao> transacoes, LocalDate referencia, int quantidadeMeses) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> receitas = new ArrayList<>();
        List<BigDecimal> despesas = new ArrayList<>();

        YearMonth ymAtual = YearMonth.from(referencia);

        for (int i = quantidadeMeses - 1; i >= 0; i--) {
            YearMonth ym = ymAtual.minusMonths(i);
            int mes = ym.getMonthValue();
            int ano = ym.getYear();

            String label = MESES_PT[mes - 1] + "/" + String.valueOf(ano).substring(2);
            labels.add(label);

            BigDecimal totalReceitas = BigDecimal.ZERO;
            BigDecimal totalDespesas = BigDecimal.ZERO;

            for (Transacao t : transacoes) {
                if (t.getStatus() == StatusTransacao.PAGA && t.getDataTransacao() != null) {
                    if (t.getDataTransacao().getMonthValue() == mes && t.getDataTransacao().getYear() == ano) {
                        BigDecimal val = t.getValor() != null ? t.getValor() : BigDecimal.ZERO;
                        if (t.getTipo() == TipoTransacao.RECEITA) {
                            totalReceitas = totalReceitas.add(val);
                        } else if (t.getTipo() == TipoTransacao.DESPESA) {
                            totalDespesas = totalDespesas.add(val);
                        }
                    }
                }
            }

            receitas.add(totalReceitas);
            despesas.add(totalDespesas);
        }

        return new EvolucaoMensalDto(labels, receitas, despesas);
    }
}
