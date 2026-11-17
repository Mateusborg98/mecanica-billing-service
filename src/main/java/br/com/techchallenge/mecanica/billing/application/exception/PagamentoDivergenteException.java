package br.com.techchallenge.mecanica.billing.application.exception;
public class PagamentoDivergenteException extends RuntimeException {
    public PagamentoDivergenteException() { super("Pagamento divergente da cobrança ou do checkout registrado."); }
}
