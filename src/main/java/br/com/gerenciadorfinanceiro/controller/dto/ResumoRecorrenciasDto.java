package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record ResumoRecorrenciasDto(
        BigDecimal totalDespesasFixas,
        BigDecimal totalReceitasRecorrentes,
        long totalAtivas
) {
}
