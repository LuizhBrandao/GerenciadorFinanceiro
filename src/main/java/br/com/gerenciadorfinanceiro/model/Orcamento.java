package br.com.gerenciadorfinanceiro.model;

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
@Setter
@NoArgsConstructor
public class Orcamento {

    @Id
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
        this.categoria = categoria;
        this.ano = ano;
        this.mes = mes;
        this.valorLimite = valorLimite;
        this.observacao = observacao;
    }
}

