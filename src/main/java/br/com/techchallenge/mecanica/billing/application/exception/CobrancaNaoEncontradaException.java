package br.com.techchallenge.mecanica.billing.application.exception;
public class CobrancaNaoEncontradaException extends RuntimeException {
    public CobrancaNaoEncontradaException() { super("Cobrança não encontrada."); }
}
