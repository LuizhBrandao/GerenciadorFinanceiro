package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(length = 255)
    private String descricao;

    @Column(length = 30)
    private String icone;

    public Categoria(Usuario usuario, String nome, TipoTransacao tipo, String descricao, String icone) {
        this.usuario = usuario;
        this.nome = nome;
        this.tipo = tipo;
        this.descricao = descricao;
        this.icone = icone;
    }
}

