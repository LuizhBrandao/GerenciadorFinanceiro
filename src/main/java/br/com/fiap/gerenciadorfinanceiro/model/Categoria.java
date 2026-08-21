package br.com.fiap.gerenciadorfinanceiro.model;

import br.com.fiap.gerenciadorfinanceiro.model.enums.TipoTransacao;
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

    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(length = 255)
    private String descricao;

    @Column(length = 30)
    private String icone;

    public Categoria(String nome, TipoTransacao tipo, String descricao, String icone) {
        this.nome = nome;
        this.tipo = tipo;
        this.descricao = descricao;
        this.icone = icone;
    }
}
