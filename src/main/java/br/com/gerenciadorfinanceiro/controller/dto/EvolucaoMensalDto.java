package br.com.gerenciadorfinanceiro.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record EvolucaoMensalDto(
        List<String> labels,
        List<BigDecimal> receitas,
        List<BigDecimal> despesas
) {
}
