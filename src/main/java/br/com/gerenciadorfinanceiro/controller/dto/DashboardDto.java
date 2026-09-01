package br.com.gerenciadorfinanceiro.controller.dto;

import java.util.List;

public record DashboardDto(
        ResumoSaldosDto saldos,
        MetricasMesDto metricasMes,
        List<TransacaoResumoDto> transacoesRecentes,
        List<DespesaPorCategoriaDto> despesasPorCategoria,
        EvolucaoMensalDto evolucaoMensal
) {
}
