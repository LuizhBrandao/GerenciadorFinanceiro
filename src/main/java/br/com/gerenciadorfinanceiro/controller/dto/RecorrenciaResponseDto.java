package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecorrenciaResponseDto(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        FrequenciaRecorrencia frequencia,
        Integer diaVencimento,
        ContaResponseDto conta,
        CategoriaResponseDto categoria,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalDate ultimoLancamento,
        Boolean ativo,
        String observacao
) {
}
