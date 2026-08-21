package br.com.fiap.gerenciadorfinanceiro.model;

import br.com.fiap.gerenciadorfinanceiro.exception.SaldoInsuficienteException;
import br.com.fiap.gerenciadorfinanceiro.model.enums.TipoConta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contas")
@Getter
@Setter
@NoArgsConstructor
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 50)
    private String instituicaoFinanceira;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoConta tipoConta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Conta(String nome, String instituicaoFinanceira, TipoConta tipoConta, BigDecimal saldoInicial) {
        this.nome = nome;
        this.instituicaoFinanceira = instituicaoFinanceira;
        this.tipoConta = tipoConta;
        this.saldo = (saldoInicial != null) ? saldoInicial : BigDecimal.ZERO;
        this.ativo = true;
        this.dataCriacao = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.saldo == null) {
            this.saldo = BigDecimal.ZERO;
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
    }

    // Métodos de Comportamento e Regras de Negócio (POO & Encapsulamento)
    public void creditar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor para crédito deve ser maior que zero.");
        }
        this.saldo = this.saldo.add(valor);
    }

    public void debitar(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor para débito deve ser maior que zero.");
        }
        if (this.saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                    String.format("Saldo insuficiente na conta '%s'. Saldo atual: R$ %.2f, Valor requerido: R$ %.2f",
                            this.nome, this.saldo, valor));
        }
        this.saldo = this.saldo.subtract(valor);
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }
}
