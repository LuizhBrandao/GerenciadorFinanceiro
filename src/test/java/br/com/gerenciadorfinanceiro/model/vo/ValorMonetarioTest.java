package br.com.gerenciadorfinanceiro.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValorMonetarioTest {

    @Test
    @DisplayName("Deve parcelar valor com dízima alocando centavo residual na primeira parcela")
    void deveParcelarSemPerdaDeCentavos() {
        ValorMonetario cemReais = ValorMonetario.of(new BigDecimal("100.00"));
        List<ValorMonetario> parcelas = cemReais.parcelar(3);

        assertEquals(3, parcelas.size());
        assertEquals(new BigDecimal("33.34"), parcelas.get(0).quantia());
        assertEquals(new BigDecimal("33.33"), parcelas.get(1).quantia());
        assertEquals(new BigDecimal("33.33"), parcelas.get(2).quantia());

        ValorMonetario soma = parcelas.stream().reduce(ValorMonetario.ZERO, ValorMonetario::somar);
        assertEquals(cemReais, soma);
    }

    @Test
    @DisplayName("Deve rejeitar valores negativos ou zero em ofPositivo")
    void deveRejeitarValorNaoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> ValorMonetario.ofPositivo(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> ValorMonetario.ofPositivo(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("Deve validar CompetenciaMensal e DiaVencimento em anos bissextos e meses curtos")
    void deveValidarCompetenciaEDiaVencimento() {
        CompetenciaMensal comp = new CompetenciaMensal(2, 2026);
        assertEquals(LocalDate.of(2026, 2, 1), comp.primeiroDia());
        assertEquals(LocalDate.of(2026, 2, 28), comp.ultimoDia());

        DiaVencimento dia31 = new DiaVencimento(31);
        assertEquals(LocalDate.of(2026, 2, 28), dia31.ajustarParaCompetencia(2026, 2));
        assertEquals(LocalDate.of(2024, 2, 29), dia31.ajustarParaCompetencia(2024, 2));

        assertThrows(IllegalArgumentException.class, () -> new CompetenciaMensal(13, 2026));
        assertThrows(IllegalArgumentException.class, () -> new DiaVencimento(32));
    }
}
