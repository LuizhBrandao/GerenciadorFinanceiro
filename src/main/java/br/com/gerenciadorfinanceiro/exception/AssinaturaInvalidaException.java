package br.com.gerenciadorfinanceiro.exception;

public class AssinaturaInvalidaException extends RuntimeException {

    public AssinaturaInvalidaException(String mensagem) {
        super(mensagem);
    }

    public AssinaturaInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
