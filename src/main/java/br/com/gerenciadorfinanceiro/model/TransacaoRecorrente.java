package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.FrequenciaRecorrencia;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import br.com.gerenciadorfinanceiro.model.vo.DiaVencimento;
import br.com.gerenciadorfinanceiro.model.vo.ValorMonetario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacoes_recorrentes")
@Getter
@NoArgsConstructor
public class TransacaoRecorrente {

    @Id
    @Setter
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
    private FrequenciaRecorrencia frequencia = FrequenciaRecorrencia.MENSAL;

    @Column(nullable = false)
    private Integer diaVencimento = 1;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column
    private LocalDate dataFim;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column
    private LocalDate ultimoLancamento;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public TransacaoRecorrente(Usuario usuario, String descricao, BigDecimal valor, TipoTransacao tipo,
                               FrequenciaRecorrencia frequencia, Integer diaVencimento, Conta conta,
                               Categoria categoria, LocalDate dataInicio, LocalDate dataFim, String observacao) {
        this.usuario = usuario;
        this.descricao = descricao;
        this.valor = ValorMonetario.ofPositivo(valor).quantia();
        this.tipo = tipo;
        this.frequencia = (frequencia != null) ? frequencia : FrequenciaRecorrencia.MENSAL;
        this.diaVencimento = new DiaVencimento(diaVencimento != null ? Math.min(Math.max(diaVencimento, 1), 31) : 1).dia();
        this.conta = conta;
        this.categoria = categoria;
        this.dataInicio = (dataInicio != null) ? dataInicio : LocalDate.now();
        this.dataFim = dataFim;
        this.ativo = true;
        this.observacao = observacao;
        this.dataCriacao = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.dataInicio == null) {
            this.dataInicio = LocalDate.now();
        }
        if (this.frequencia == null) {
            this.frequencia = FrequenciaRecorrencia.MENSAL;
        }
        if (this.diaVencimento == null || this.diaVencimento < 1) {
            this.diaVencimento = 1;
        }
        if (this.ativo == null) {
            this.ativo = true;
        }
    }

    public void atualizarPlano(String descricao, BigDecimal valor, TipoTransacao tipo,
                               FrequenciaRecorrencia frequencia, Integer diaVencimento,
                               Conta conta, Categoria categoria, LocalDate dataInicio,
                               LocalDate dataFim, String observacao) {
        this.descricao = descricao;
        this.valor = ValorMonetario.ofPositivo(valor).quantia();
        this.tipo = tipo;
        this.frequencia = (frequencia != null) ? frequencia : FrequenciaRecorrencia.MENSAL;
        this.diaVencimento = new DiaVencimento(diaVencimento != null ? Math.min(Math.max(diaVencimento, 1), 31) : 1).dia();
        this.conta = conta;
        this.categoria = categoria;
        this.dataInicio = (dataInicio != null) ? dataInicio : LocalDate.now();
        this.dataFim = dataFim;
        this.observacao = observacao;
    }

    public void alternarAtivacao() {
        this.ativo = !Boolean.TRUE.equals(this.ativo);
    }

    public void registrarUltimoLancamento(LocalDate dataLancamento) {
        this.ultimoLancamento = dataLancamento;
    }
}
