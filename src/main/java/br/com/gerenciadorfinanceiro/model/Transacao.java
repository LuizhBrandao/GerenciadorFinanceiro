package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.StatusTransacao;
import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
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
@Table(name = "transacoes")
@Getter
@NoArgsConstructor
public class Transacao {

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
    private StatusTransacao status = StatusTransacao.PAGA;

    @Column(nullable = false)
    private LocalDate dataTransacao;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorrencia_id")
    private TransacaoRecorrente recorrencia;

    @Column(name = "chave_recorrencia", length = 100, unique = true)
    private String chaveRecorrencia;

    @Column(name = "transferencia", columnDefinition = "boolean default false")
    private Boolean transferencia = false;

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
        this.transferencia = false;
        this.dataRegistro = LocalDateTime.now();
    }

    public static Transacao criarSaidaTransferencia(Conta origem, Conta destino, BigDecimal valor, Categoria categoria) {
        BigDecimal quantia = ValorMonetario.ofPositivo(valor).quantia();
        Transacao saida = new Transacao(
                origem.getUsuario(),
                "Transferência enviada para " + destino.getNome(),
                quantia,
                TipoTransacao.DESPESA,
                StatusTransacao.PAGA,
                LocalDate.now(),
                origem,
                categoria,
                "Transferência patrimonial entre contas"
        );
        saida.transferencia = true;
        return saida;
    }

    public static Transacao criarEntradaTransferencia(Conta origem, Conta destino, BigDecimal valor, Categoria categoria) {
        BigDecimal quantia = ValorMonetario.ofPositivo(valor).quantia();
        Transacao entrada = new Transacao(
                destino.getUsuario(),
                "Transferência recebida de " + origem.getNome(),
                quantia,
                TipoTransacao.RECEITA,
                StatusTransacao.PAGA,
                LocalDate.now(),
                destino,
                categoria,
                "Transferência patrimonial entre contas"
        );
        entrada.transferencia = true;
        return entrada;
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
        if (this.transferencia == null) {
            this.transferencia = false;
        }
    }

    public boolean isTransferenciaPatrimonial() {
        return Boolean.TRUE.equals(this.transferencia);
    }

    public void liquidar() {
        this.status = StatusTransacao.PAGA;
    }

    public void estornarParaPendente() {
        this.status = StatusTransacao.PENDENTE;
    }

    public void retificar(String descricao, BigDecimal valor, TipoTransacao tipo, StatusTransacao status,
                          LocalDate dataTransacao, Conta conta, Categoria categoria, String observacao) {
        this.descricao = descricao;
        this.valor = ValorMonetario.ofPositivo(valor).quantia();
        this.tipo = tipo;
        this.status = (status != null) ? status : StatusTransacao.PAGA;
        this.dataTransacao = (dataTransacao != null) ? dataTransacao : LocalDate.now();
        this.conta = conta;
        this.categoria = categoria;
        this.observacao = observacao;
    }

    public void vincularRecorrencia(TransacaoRecorrente recorrencia) {
        this.recorrencia = recorrencia;
    }

    public void sincronizarComRecorrencia(TransacaoRecorrente recorrencia) {
        this.recorrencia = recorrencia;
        this.descricao = recorrencia.getDescricao();
        this.categoria = recorrencia.getCategoria();
        this.conta = recorrencia.getConta();
        this.tipo = recorrencia.getTipo();
        if (this.status == StatusTransacao.PENDENTE && recorrencia.getValor() != null) {
            this.valor = ValorMonetario.ofPositivo(recorrencia.getValor()).quantia();
        }
    }

    public void definirTags(java.util.Set<Tag> tags) {
        this.tags = (tags != null) ? new java.util.HashSet<>(tags) : new java.util.HashSet<>();
    }

    public void definirChaveRecorrencia(String chaveRecorrencia) {
        this.chaveRecorrencia = chaveRecorrencia;
    }
}

