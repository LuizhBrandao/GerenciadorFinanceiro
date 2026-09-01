package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record MetricasMesDto(
        BigDecimal receitasMes,
        BigDecimal despesasMes,
        BigDecimal balancoMes,
        BigDecimal receitasPendentes,
        BigDecimal despesasPendentes
) {
}
