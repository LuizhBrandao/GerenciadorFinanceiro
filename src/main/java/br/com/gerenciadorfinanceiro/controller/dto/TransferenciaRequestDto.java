package br.com.gerenciadorfinanceiro.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequestDto(
        @NotNull(message = "Conta de origem é obrigatória.")
        Long contaOrigemId,

        @NotNull(message = "Conta de destino é obrigatória.")
        Long contaDestinoId,

        @NotNull(message = "Valor da transferência é obrigatório.")
        @Positive(message = "O valor deve ser positivo e maior que zero.")
        BigDecimal valor
) {
}
