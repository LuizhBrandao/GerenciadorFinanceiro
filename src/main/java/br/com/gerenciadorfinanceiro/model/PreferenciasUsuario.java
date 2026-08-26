package br.com.gerenciadorfinanceiro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "preferencias_usuario")
@Getter
@Setter
@NoArgsConstructor
public class PreferenciasUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 10)
    private String moedaPadrao = "BRL";

    @Column(nullable = false)
    private boolean receberNotificacoes = true;

    @Column(nullable = false, length = 20)
    private String tema = "LIGHT";

    public PreferenciasUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
