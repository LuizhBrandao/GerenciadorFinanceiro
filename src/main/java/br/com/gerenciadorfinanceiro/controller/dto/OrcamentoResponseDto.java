package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record OrcamentoResponseDto(
        Long id,
        CategoriaResponseDto categoria,
        Integer ano,
        Integer mes,
        BigDecimal valorLimite,
        BigDecimal valorGasto,
        BigDecimal percentualConsumido,
        boolean ultrapassado,
        String observacao
) {
}
