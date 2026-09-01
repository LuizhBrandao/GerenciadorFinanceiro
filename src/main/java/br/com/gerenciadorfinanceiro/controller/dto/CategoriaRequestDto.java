package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDto(
        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres.")
        String nome,

        @NotNull(message = "O tipo da transação é obrigatório.")
        TipoTransacao tipo,

        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres.")
        String descricao,

        @Size(max = 50, message = "O ícone deve ter no máximo 50 caracteres.")
        String icone
) {
}
