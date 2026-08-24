package br.com.gerenciadorfinanceiro.model.enums;

public enum StatusTransacao {
    PAGA("Paga"),
    PENDENTE("Pendente"),
    ATRASADA("Atrasada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

