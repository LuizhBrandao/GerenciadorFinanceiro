package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.vo.CompetenciaMensal;
import br.com.gerenciadorfinanceiro.model.vo.ValorMonetario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamentos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"categoria_id", "ano", "mes"})
})
@Getter
@NoArgsConstructor
public class Orcamento {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorLimite;

    @Column(length = 255)
    private String observacao;

    public Orcamento(Categoria categoria, Integer ano, Integer mes, BigDecimal valorLimite, String observacao) {
        CompetenciaMensal competencia = new CompetenciaMensal(mes, ano);
        this.categoria = categoria;
        this.ano = competencia.ano();
        this.mes = competencia.mes();
        this.valorLimite = ValorMonetario.ofPositivo(valorLimite).quantia();
        this.observacao = observacao;
    }

    public void atualizarLimite(BigDecimal novoLimite, String novaObservacao) {
        this.valorLimite = ValorMonetario.ofPositivo(novoLimite).quantia();
        this.observacao = novaObservacao;
    }

    public boolean ultrapassouLimite(BigDecimal totalGasto) {
        if (totalGasto == null) return false;
        return totalGasto.compareTo(this.valorLimite) > 0;
    }
}

