package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record ResumoSaldosDto(
        BigDecimal saldoContasCorrentes,
        BigDecimal saldoInvestimentos,
        BigDecimal patrimonioTotal,
        long totalContasCorrentes,
        long totalContasInvestimento
) {
}
