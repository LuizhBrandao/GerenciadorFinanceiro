package br.com.gerenciadorfinanceiro.model;

import br.com.gerenciadorfinanceiro.model.enums.TipoTransacao;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categorias_usuario_nome_tipo", columnNames = {"usuario_id", "nome", "tipo"})
})
@Getter
@NoArgsConstructor
public class Categoria {

    @Id
    @Setter
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
        this.icone = (icone != null && !icone.isBlank()) ? icone : "fa-tag";
    }

    public static java.util.List<Categoria> criarCatalogoPadrao(Usuario usuario) {
        return java.util.List.of(
                new Categoria(usuario, "Salário e Remuneração", TipoTransacao.RECEITA,
                        "Salário fixo mensal, adiantamentos, 13º salário e benefícios em folha.", "fa-briefcase"),
                new Categoria(usuario, "Rendimentos & Investimentos", TipoTransacao.RECEITA,
                        "Dividendos, juros sobre capital próprio (JCP), rendimentos de CDI/Poupança e fundos imobiliários.", "fa-chart-line"),
                new Categoria(usuario, "Freelance & Serviços Extras", TipoTransacao.RECEITA,
                        "Trabalhos autônomos, consultorias, projetos paralelos e vendas pontuais.", "fa-laptop"),
                new Categoria(usuario, "Moradia & Habitação", TipoTransacao.DESPESA,
                        "Aluguel, condomínio, IPTU, contas essenciais (energia elétrica, água, gás, internet).", "fa-house"),
                new Categoria(usuario, "Alimentação & Supermercado", TipoTransacao.DESPESA,
                        "Compras de supermercado, feira, açougue, padaria e delivery/refeições do dia a dia.", "fa-utensils"),
                new Categoria(usuario, "Transporte & Mobilidade", TipoTransacao.DESPESA,
                        "Combustível, transporte público, corridas por aplicativo (Uber/99), estacionamento, IPVA e manutenção veicular.", "fa-car"),
                new Categoria(usuario, "Saúde & Bem-Estar", TipoTransacao.DESPESA,
                        "Plano de saúde, consultas, farmácia/medicamentos, exames e academia/atividades físicas.", "fa-heart-pulse"),
                new Categoria(usuario, "Educação & Desenvolvimento", TipoTransacao.DESPESA,
                        "Mensalidades escolares/faculdade, cursos online, livros, certificações e workshops.", "fa-graduation-cap"),
                new Categoria(usuario, "Lazer & Entretenimento", TipoTransacao.DESPESA,
                        "Assinaturas de streaming (Netflix, Spotify), restaurantes/bares, viagens, cinema e passeios.", "fa-ticket"),
                new Categoria(usuario, "Cuidados Pessoais & Compras", TipoTransacao.DESPESA,
                        "Roupas, calçados, barbearia/salão de beleza, cosméticos e itens de uso pessoal.", "fa-bag-shopping"),
                new Categoria(usuario, "Contas Básicas & Energia", TipoTransacao.DESPESA,
                        "Contas de energia, água, luz, gás e taxas.", "fa-bolt"),
                new Categoria(usuario, "Outros", TipoTransacao.DESPESA,
                        "Outros gastos e despesas gerais.", "fa-tag"),
                new Categoria(usuario, "Outras Receitas", TipoTransacao.RECEITA,
                        "Outras entradas e ganhos gerais.", "fa-tag")
        );
    }
}

