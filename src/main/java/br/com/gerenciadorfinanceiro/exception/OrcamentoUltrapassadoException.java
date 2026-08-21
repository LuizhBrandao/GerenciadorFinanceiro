package br.com.gerenciadorfinanceiro.exception;

public class OrcamentoUltrapassadoException extends RuntimeException {

    public OrcamentoUltrapassadoException(String message) {
        super(message);
    }
}

