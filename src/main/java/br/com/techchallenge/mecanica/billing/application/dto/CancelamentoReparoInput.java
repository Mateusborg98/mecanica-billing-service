package br.com.techchallenge.mecanica.billing.application.dto;
import java.time.Instant;
import java.util.UUID;
public record CancelamentoReparoInput(int schemaVersion,String eventType,UUID eventId,UUID reparoId,UUID ordemId,Instant ocorridoEm) {
    public CancelamentoReparoInput {
        if(schemaVersion!=1 || !"ReparoCancelado".equals(eventType) || reparoId==null || !reparoId.equals(eventId)
            || ordemId==null || ocorridoEm==null || !ocorridoEm.equals(ocorridoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
            throw new IllegalArgumentException("Cancelamento inválido");
    }
    public UUID orcamentoId() {
        return UUID.nameUUIDFromBytes(("mecanica:diagnostico:v1:"+reparoId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
