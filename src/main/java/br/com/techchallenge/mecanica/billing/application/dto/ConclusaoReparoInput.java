package br.com.techchallenge.mecanica.billing.application.dto;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
public record ConclusaoReparoInput(UUID eventId, UUID reparoId, UUID ordemId, UUID orcamentoId, Instant ocorridoEm) {
    public ConclusaoReparoInput {
        if(eventId==null || reparoId==null || ordemId==null || orcamentoId==null || ocorridoEm==null)
            throw new IllegalArgumentException("Identificadores e data de conclusão são obrigatórios.");
        var esperado=UUID.nameUUIDFromBytes(("mecanica:diagnostico:v1:"+reparoId).getBytes(StandardCharsets.UTF_8));
        if(!eventId.equals(reparoId) || !esperado.equals(orcamentoId))
            throw new IllegalArgumentException("Conclusão não corresponde ao diagnóstico/orçamento.");
        ocorridoEm=ocorridoEm.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }
}
