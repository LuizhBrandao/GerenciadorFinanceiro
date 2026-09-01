package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record DespesaPorCategoriaDto(
        Long categoriaId,
        String nome,
        String icone,
        BigDecimal total,
        BigDecimal percentual
) {
}
