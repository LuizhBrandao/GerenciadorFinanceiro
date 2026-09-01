package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecorrenciaRequestDto(
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres.")
        String descricao,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser positivo e maior que zero.")
        BigDecimal valor,

        @NotNull(message = "O tipo da transação é obrigatório.")
        TipoTransacao tipo,

        FrequenciaRecorrencia frequencia,

        @Min(value = 1, message = "Dia de vencimento deve ser no mínimo 1.")
        @Max(value = 31, message = "Dia de vencimento deve ser no máximo 31.")
        Integer diaVencimento,

        @NotNull(message = "A conta é obrigatória.")
        Long contaId,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        LocalDate dataInicio,
        LocalDate dataFim,

        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
        String observacao
) {
}
