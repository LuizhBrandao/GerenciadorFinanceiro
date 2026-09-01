package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoContasResponseDto(
        List<ContaResponseDto> contasCorrentes,
        List<ContaResponseDto> contasInvestimentos,
        BigDecimal subtotalCorrentes,
        BigDecimal subtotalInvestimentos,
        ResumoSaldosDto resumoSaldos
) {
}
