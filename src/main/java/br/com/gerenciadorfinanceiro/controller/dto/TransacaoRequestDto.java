package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record TransacaoRequestDto(
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres.")
        String descricao,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser positivo e maior que zero.")
        BigDecimal valor,

        @NotNull(message = "O tipo da transação é obrigatório.")
        TipoTransacao tipo,

        StatusTransacao status,

        @NotNull(message = "A data da transação é obrigatória.")
        LocalDate dataTransacao,

        @NotNull(message = "A conta é obrigatória.")
        Long contaId,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
        String observacao,

        Set<String> tags
) {
}
