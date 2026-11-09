package br.com.techchallenge.mecanica.billing.application.exception;
public class CheckoutIndisponivelException extends RuntimeException {
    public CheckoutIndisponivelException() { super("Checkout indisponível. Tente novamente usando a mesma cobrança."); }
}
