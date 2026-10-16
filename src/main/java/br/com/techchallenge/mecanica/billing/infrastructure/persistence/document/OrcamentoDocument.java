package br.com.techchallenge.mecanica.billing.infrastructure.persistence.document;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "orcamentos")
public record OrcamentoDocument(
        @MongoId
        String id,
        String ordemId,
        List<ItemOrcamentoDocument> itens,
        Instant criadoEm,
        String status, String reparoId) {
    public OrcamentoDocument(String id, String ordemId, List<ItemOrcamentoDocument> itens, Instant criadoEm, String status) {
        this(id, ordemId, itens, criadoEm, status, null);
    }
    public OrcamentoDocument comReparo(java.util.UUID reparoId) {
        return new OrcamentoDocument(id, ordemId, itens, criadoEm, status, reparoId.toString());
    }
}
