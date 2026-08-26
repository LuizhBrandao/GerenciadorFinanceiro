package br.com.gerenciadorfinanceiro.model.enums;

public enum FrequenciaRecorrencia {
    DIARIA("Diária"),
    SEMANAL("Semanal"),
    QUINZENAL("Quinzenal"),
    MENSAL("Mensal"),
    ANUAL("Anual");

    private final String descricao;

    FrequenciaRecorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
