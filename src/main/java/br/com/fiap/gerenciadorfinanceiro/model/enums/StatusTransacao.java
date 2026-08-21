package br.com.fiap.gerenciadorfinanceiro.model.enums;

public enum StatusTransacao {
    CONCLUIDA("Concluída"),
    PENDENTE("Pendente"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
