package br.com.gerenciadorfinanceiro.model.enums;

public enum TipoTransacao {
    RECEITA("Receita"),
    DESPESA("Despesa");

    private final String descricao;

    TipoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

