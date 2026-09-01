package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;

public record CategoriaResponseDto(
        Long id,
        String nome,
        TipoTransacao tipo,
        String descricao,
        String icone
) {
}
