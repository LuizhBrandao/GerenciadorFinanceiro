package br.com.gerenciadorfinanceiro.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de Valor (Value Object) imutável que representa uma quantia monetária no domínio financeiro.
 * Garante escala padronizada de 2 casas decimais e distribuição contábil exata em parcelamentos.
 */
public record ValorMonetario(BigDecimal quantia) {

    public static final ValorMonetario ZERO = new ValorMonetario(BigDecimal.ZERO);

    public ValorMonetario {
        Objects.requireNonNull(quantia, "O valor monetário não pode ser nulo.");
        quantia = quantia.setScale(2, RoundingMode.HALF_UP);
    }

    public static ValorMonetario of(BigDecimal valor) {
        return new ValorMonetario(valor);
    }

    public static ValorMonetario ofPositivo(BigDecimal valor) {
        ValorMonetario vo = new ValorMonetario(valor);
        if (!vo.ehPositivo()) {
            throw new IllegalArgumentException("O valor monetário deve ser maior que zero.");
        }
        return vo;
    }

    public boolean ehPositivo() {
        return this.quantia.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean ehZero() {
        return this.quantia.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean ehMenorQue(ValorMonetario outro) {
        return this.quantia.compareTo(outro.quantia) < 0;
    }

    public ValorMonetario somar(ValorMonetario outro) {
        return new ValorMonetario(this.quantia.add(outro.quantia));
    }

    public ValorMonetario subtrair(ValorMonetario outro) {
        return new ValorMonetario(this.quantia.subtract(outro.quantia));
    }

    /**
     * Divide o valor monetário em N parcelas garantindo que a soma das parcelas seja
     * estritamente igual ao valor total original (alocando eventual resíduo de centavos na 1ª parcela).
     */
    public List<ValorMonetario> parcelar(int quantidadeParcelas) {
        if (quantidadeParcelas <= 0) {
            throw new IllegalArgumentException("A quantidade de parcelas deve ser de no mínimo 1.");
        }
        if (!this.ehPositivo()) {
            throw new IllegalArgumentException("O valor total para parcelamento deve ser maior que zero.");
        }

        BigDecimal divisor = BigDecimal.valueOf(quantidadeParcelas);
        BigDecimal valorBase = this.quantia.divide(divisor, 2, RoundingMode.DOWN);
        BigDecimal somaBase = valorBase.multiply(divisor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal residuoCentavos = this.quantia.subtract(somaBase);

        List<ValorMonetario> parcelas = new ArrayList<>(quantidadeParcelas);
        for (int i = 0; i < quantidadeParcelas; i++) {
            if (i == 0) {
                parcelas.add(new ValorMonetario(valorBase.add(residuoCentavos)));
            } else {
                parcelas.add(new ValorMonetario(valorBase));
            }
        }
        return Collections.unmodifiableList(parcelas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValorMonetario that)) return false;
        return this.quantia.compareTo(that.quantia) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.quantia.stripTrailingZeros());
    }
}
