package br.com.gerenciadorfinanceiro.model.vo;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Objeto de Valor (Value Object) imutável que representa uma Competência Orçamentária/Contábil (Mês e Ano).
 */
public record CompetenciaMensal(int mes, int ano) {

    public CompetenciaMensal {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("O mês da competência deve estar entre 1 e 12.");
        }
        if (ano < 1970 || ano > 2100) {
            throw new IllegalArgumentException("O ano da competência deve estar entre 1970 e 2100.");
        }
    }

    public static CompetenciaMensal de(LocalDate data) {
        return new CompetenciaMensal(data.getMonthValue(), data.getYear());
    }

    public static CompetenciaMensal atual() {
        return de(LocalDate.now());
    }

    public YearMonth toYearMonth() {
        return YearMonth.of(ano, mes);
    }

    public LocalDate primeiroDia() {
        return toYearMonth().atDay(1);
    }

    public LocalDate ultimoDia() {
        return toYearMonth().atEndOfMonth();
    }
}
