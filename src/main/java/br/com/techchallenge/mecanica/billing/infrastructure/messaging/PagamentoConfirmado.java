package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
public record PagamentoConfirmado(int schemaVersion,String eventType,UUID eventId,UUID cobrancaId,
        UUID ordemId,UUID reparoId,BigDecimal valor,String moeda,Instant confirmadoEm) {}
