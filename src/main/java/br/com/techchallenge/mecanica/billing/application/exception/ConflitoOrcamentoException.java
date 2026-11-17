package br.com.techchallenge.mecanica.billing.application.exception;
import java.util.UUID;
public class ConflitoOrcamentoException extends RuntimeException {
    public ConflitoOrcamentoException(UUID id) {
        super("O orçamento " + id + " já foi alterado. Consulte o estado atual.");
    }
}
