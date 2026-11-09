package br.com.techchallenge.mecanica.billing.application.gateway;
import java.time.Instant;
import java.util.UUID;
public interface NotificacaoOrcamentoGateway {
    record Situacao(UUID orcamentoId, String status, int tentativas) {}
    void agendar(UUID id, String destinatario, String texto, Instant agora);
    Situacao buscar(UUID id);
}
