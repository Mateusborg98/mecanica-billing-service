package br.com.techchallenge.mecanica.billing.infrastructure.persistence.document;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
public record CobrancaDocument(String eventoId, String reparoId, String ordemId,
        @Field(targetType=FieldType.DECIMAL128) BigDecimal valorTotal, String moeda, String status,
        Instant reparoConcluidoEm, Instant criadaEm) { }
