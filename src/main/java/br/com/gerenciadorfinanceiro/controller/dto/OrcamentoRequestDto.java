package br.com.gerenciadorfinanceiro.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrcamentoRequestDto(
        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "O ano é obrigatório.")
        @Min(value = 2000, message = "O ano deve ser no mínimo 2000.")
        @Max(value = 2100, message = "O ano deve ser no máximo 2100.")
        Integer ano,

        @NotNull(message = "O mês é obrigatório.")
        @Min(value = 1, message = "O mês deve ser entre 1 e 12.")
        @Max(value = 12, message = "O mês deve ser entre 1 e 12.")
        Integer mes,

        @NotNull(message = "O valor limite é obrigatório.")
        @Positive(message = "O valor limite deve ser maior que zero.")
        BigDecimal valorLimite,

        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
        String observacao
) {
}
