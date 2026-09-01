package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTransacao status = StatusTransacao.PAGA;

    @Column(nullable = false)
    private LocalDate dataTransacao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorrencia_id")
    private TransacaoRecorrente recorrencia;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "transacao_tags",
        joinColumns = @JoinColumn(name = "transacao_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private java.util.Set<Tag> tags = new java.util.HashSet<>();

    public Transacao(Usuario usuario, String descricao, BigDecimal valor, TipoTransacao tipo, StatusTransacao status,
                     LocalDate dataTransacao, Conta conta, Categoria categoria, String observacao) {
        this.usuario = usuario;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.status = (status != null) ? status : StatusTransacao.PAGA;
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
            this.status = StatusTransacao.PAGA;
        }
    }
}

