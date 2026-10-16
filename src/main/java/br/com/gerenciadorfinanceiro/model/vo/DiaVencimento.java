package br.com.gerenciadorfinanceiro.model.vo;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Objeto de Valor (Value Object) imutável que representa o Dia de Vencimento (1 a 31)
 * e resolve o ajuste automático para meses mais curtos (ex.: fevereiro).
 */
public record DiaVencimento(int dia) {

    public DiaVencimento {
        if (dia < 1 || dia > 31) {
            throw new IllegalArgumentException("O dia de vencimento deve estar entre 1 e 31.");
        }
    }

    public LocalDate ajustarParaCompetencia(int ano, int mes) {
        YearMonth ym = YearMonth.of(ano, mes);
        int diaAjustado = Math.min(this.dia, ym.lengthOfMonth());
        return LocalDate.of(ano, mes, diaAjustado);
    }
}
