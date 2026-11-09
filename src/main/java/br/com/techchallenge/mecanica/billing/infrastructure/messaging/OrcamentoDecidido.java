package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import java.time.Instant;
import java.util.UUID;
public record OrcamentoDecidido(int schemaVersion, String eventType, UUID eventId, UUID orcamentoId,
        UUID reparoId, UUID ordemId, String decisao, Instant ocorridoEm) { }
