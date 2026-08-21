package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacoes")
@Getter
@Setter
@NoArgsConstructor
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTransacao status = StatusTransacao.CONCLUIDA;

    @Column(nullable = false)
    private LocalDate dataTransacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    public Transacao(String descricao, BigDecimal valor, TipoTransacao tipo, StatusTransacao status,
                     LocalDate dataTransacao, Conta conta, Categoria categoria, String observacao) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.status = (status != null) ? status : StatusTransacao.CONCLUIDA;
        this.dataTransacao = (dataTransacao != null) ? dataTransacao : LocalDate.now();
        this.conta = conta;
        this.categoria = categoria;
        this.observacao = observacao;
        this.dataRegistro = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataRegistro == null) {
            this.dataRegistro = LocalDateTime.now();
        }
        if (this.dataTransacao == null) {
            this.dataTransacao = LocalDate.now();
        }
        if (this.status == null) {
            this.status = StatusTransacao.CONCLUIDA;
        }
    }
}

