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
        String status) {
}
