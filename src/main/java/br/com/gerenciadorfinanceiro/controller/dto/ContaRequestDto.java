package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ContaRequestDto(
        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @Size(max = 50, message = "A instituição financeira deve ter no máximo 50 caracteres.")
        String instituicaoFinanceira,

        @NotNull(message = "O tipo de conta é obrigatório.")
        TipoConta tipoConta,

        BigDecimal saldo
) {
}
