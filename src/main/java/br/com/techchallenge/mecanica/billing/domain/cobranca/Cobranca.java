package br.com.techchallenge.mecanica.billing.domain.cobranca;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
public record Cobranca(UUID id, UUID ordemId, UUID reparoId, BigDecimal valorTotal, String moeda,
        Status status, Instant reparoConcluidoEm, Instant criadaEm) {
    public enum Status { PENDENTE }
    public Cobranca {
        Objects.requireNonNull(id); Objects.requireNonNull(ordemId); Objects.requireNonNull(reparoId);
        Objects.requireNonNull(valorTotal); Objects.requireNonNull(status);
        Objects.requireNonNull(reparoConcluidoEm); Objects.requireNonNull(criadaEm);
        if(valorTotal.signum() <= 0 || !"BRL".equals(moeda)) throw new IllegalArgumentException("Valor ou moeda inválidos.");
    }
}
