package br.com.techchallenge.mecanica.billing.infrastructure.persistence.document;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public record ItemOrcamentoDocument(
        String referenciaId,
        String tipo,
        String descricao,
        int quantidade,
        @Field(targetType = FieldType.DECIMAL128)
        BigDecimal valorUnitario) {
}
