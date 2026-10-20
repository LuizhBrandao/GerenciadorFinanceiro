package br.com.gerenciadorfinanceiro.service.impl;

import br.com.gerenciadorfinanceiro.controller.dto.DashboardDto;
import br.com.gerenciadorfinanceiro.controller.dto.DespesaPorCategoriaDto;
import br.com.gerenciadorfinanceiro.controller.dto.EvolucaoMensalDto;
import br.com.gerenciadorfinanceiro.controller.dto.MetricasMesDto;
import br.com.gerenciadorfinanceiro.controller.dto.ResumoSaldosDto;
import br.com.gerenciadorfinanceiro.controller.dto.TransacaoResumoDto;
import br.com.gerenciadorfinanceiro.controller.mapper.DtoMapper;
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
import java.util.HashMap;
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

        LocalDate hoje = LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();

        MetricasMesDto metricasMes = calcularMetricasMes(usuarioId, mesAtual, anoAtual);
        List<TransacaoResumoDto> transacoesRecentes = transacaoRepository
                .findTop5ByUsuarioIdAndDataTransacaoLessThanEqualOrderByDataTransacaoDescIdDesc(usuarioId, hoje)
                .stream()
                .map(DtoMapper::toTransacaoResumo)
                .collect(Collectors.toList());
        List<DespesaPorCategoriaDto> despesasPorCategoria = calcularDespesasPorCategoria(usuarioId);
        EvolucaoMensalDto evolucaoMensal = calcularEvolucaoMensal(usuarioId, hoje, 6);

        return new DashboardDto(saldos, metricasMes, transacoesRecentes, despesasPorCategoria, evolucaoMensal);
    }

    private MetricasMesDto calcularMetricasMes(Long usuarioId, int mes, int ano) {
        BigDecimal receitasMes = transacaoRepository.somarPorTipoEStatusECompetencia(
                usuarioId, TipoTransacao.RECEITA, StatusTransacao.PAGA, ano, mes);
        BigDecimal despesasMes = transacaoRepository.somarPorTipoEStatusECompetencia(
                usuarioId, TipoTransacao.DESPESA, StatusTransacao.PAGA, ano, mes);
        BigDecimal receitasPendentes = transacaoRepository.somarPorTipoEStatusECompetencia(
                usuarioId, TipoTransacao.RECEITA, StatusTransacao.PENDENTE, ano, mes);
        BigDecimal despesasPendentes = transacaoRepository.somarPorTipoEStatusECompetencia(
                usuarioId, TipoTransacao.DESPESA, StatusTransacao.PENDENTE, ano, mes);

        BigDecimal rMes = (receitasMes != null) ? receitasMes : BigDecimal.ZERO;
        BigDecimal dMes = (despesasMes != null) ? despesasMes : BigDecimal.ZERO;
        BigDecimal rPend = (receitasPendentes != null) ? receitasPendentes : BigDecimal.ZERO;
        BigDecimal dPend = (despesasPendentes != null) ? despesasPendentes : BigDecimal.ZERO;
        BigDecimal balancoMes = rMes.subtract(dMes);

        return new MetricasMesDto(rMes, dMes, balancoMes, rPend, dPend);
    }

    private List<DespesaPorCategoriaDto> calcularDespesasPorCategoria(Long usuarioId) {
        List<Object[]> rows = transacaoRepository.agruparDespesasPagasPorCategoria(usuarioId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        BigDecimal totalGeral = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal totalCat = (BigDecimal) row[3];
            if (totalCat != null) {
                totalGeral = totalGeral.add(totalCat);
            }
        }

        List<DespesaPorCategoriaDto> resultado = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long catId = (Long) row[0];
            String nome = row[1] != null ? (String) row[1] : "Outras";
            String icone = row[2] != null ? (String) row[2] : "fa-tag";
            BigDecimal totalCat = (BigDecimal) row[3];
            if (totalCat == null) totalCat = BigDecimal.ZERO;

            BigDecimal percentual = (totalGeral.compareTo(BigDecimal.ZERO) > 0)
                    ? totalCat.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            resultado.add(new DespesaPorCategoriaDto(catId, nome, icone, totalCat, percentual));
        }

        return resultado;
    }

    private EvolucaoMensalDto calcularEvolucaoMensal(Long usuarioId, LocalDate referencia, int quantidadeMeses) {
        List<String> labels = new ArrayList<>(quantidadeMeses);
        List<BigDecimal> receitas = new ArrayList<>(quantidadeMeses);
        List<BigDecimal> despesas = new ArrayList<>(quantidadeMeses);

        YearMonth ymAtual = YearMonth.from(referencia);
        YearMonth ymInicial = ymAtual.minusMonths(quantidadeMeses - 1);

        LocalDate dataInicio = ymInicial.atDay(1);
        LocalDate dataFim = ymAtual.atEndOfMonth();

        List<Object[]> rows = transacaoRepository.somarEvolucaoMensalPorPeriodo(usuarioId, dataInicio, dataFim);
        Map<String, BigDecimal> totais = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                Integer ano = ((Number) row[0]).intValue();
                Integer mes = ((Number) row[1]).intValue();
                TipoTransacao tipo = (TipoTransacao) row[2];
                BigDecimal valor = (BigDecimal) row[3];
                String chave = ano + "-" + mes + "-" + tipo.name();
                totais.put(chave, valor != null ? valor : BigDecimal.ZERO);
            }
        }

        for (int i = quantidadeMeses - 1; i >= 0; i--) {
            YearMonth ym = ymAtual.minusMonths(i);
            int mes = ym.getMonthValue();
            int ano = ym.getYear();

            String label = MESES_PT[mes - 1] + "/" + String.valueOf(ano).substring(2);
            labels.add(label);

            BigDecimal totalReceitas = totais.getOrDefault(ano + "-" + mes + "-" + TipoTransacao.RECEITA.name(), BigDecimal.ZERO);
            BigDecimal totalDespesas = totais.getOrDefault(ano + "-" + mes + "-" + TipoTransacao.DESPESA.name(), BigDecimal.ZERO);

            receitas.add(totalReceitas);
            despesas.add(totalDespesas);
        }

        return new EvolucaoMensalDto(labels, receitas, despesas);
    }
}
