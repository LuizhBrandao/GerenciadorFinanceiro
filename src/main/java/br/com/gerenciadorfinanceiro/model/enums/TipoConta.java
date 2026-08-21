package br.com.gerenciadorfinanceiro.model.enums;

public enum TipoConta {
    CORRENTE("Conta Corrente"),
    POUPANCA("Conta Poupança"),
    INVESTIMENTO("Conta de Investimento"),
    CARTEIRA("Carteira Física / Dinheiro");

    private final String descricao;

    TipoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

