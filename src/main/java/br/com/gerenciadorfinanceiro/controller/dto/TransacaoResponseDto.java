package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record TransacaoResponseDto(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        StatusTransacao status,
        LocalDate dataTransacao,
        ContaResponseDto conta,
        CategoriaResponseDto categoria,
        String observacao,
        Set<String> tags
) {
}
