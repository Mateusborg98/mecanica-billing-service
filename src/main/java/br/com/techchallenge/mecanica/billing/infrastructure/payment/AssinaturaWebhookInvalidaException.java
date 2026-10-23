package br.com.techchallenge.mecanica.billing.infrastructure.payment;
public class AssinaturaWebhookInvalidaException extends RuntimeException {
    public AssinaturaWebhookInvalidaException() { super("Notificação não autenticada."); }
}
