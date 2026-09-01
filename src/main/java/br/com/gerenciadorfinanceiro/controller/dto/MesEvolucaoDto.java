package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;

public record MesEvolucaoDto(
        String label,
        int mes,
        int ano,
        BigDecimal receitas,
        BigDecimal despesas
) {
}
