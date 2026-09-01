package br.com.gerenciadorfinanceiro.controller.dto;

import br.com.gerenciadorfinanceiro.model.enums.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContaResponseDto(
        Long id,
        String nome,
        String instituicaoFinanceira,
        TipoConta tipoConta,
        BigDecimal saldo,
        Boolean ativo,
        LocalDateTime dataCriacao
) {
}
