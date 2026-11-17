package br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;

class OrcamentoPersistenceMapperTest {

    private final OrcamentoPersistenceMapper mapper = new OrcamentoPersistenceMapper();

    @Test
    void devePreservarOrcamentoAprovadoNaConversaoCompleta() {
        var peca = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "Filtro",
                2,
                new BigDecimal("25.50"));

        var servico = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.SERVICO,
                "Troca de filtro",
                1,
                new BigDecimal("40.00"));

        var original = new Orcamento(
                UUID.randomUUID(),
                List.of(peca, servico),
                Instant.parse("2026-10-09T12:00:00Z"));

        original.aprovar();

        var document = mapper.toDocument(original);
        var recuperado = mapper.toDomain(document);

        assertEquals(original.getId().toString(), document.id());
        assertEquals("APROVADO", document.status());
        assertEquals("PECA", document.itens().get(0).tipo());
        assertEquals("SERVICO", document.itens().get(1).tipo());

        assertEquals(original.getId(), recuperado.getId());
        assertEquals(original.getOrdemId(), recuperado.getOrdemId());
        assertEquals(original.getCriadoEm(), recuperado.getCriadoEm());
        assertEquals(original.getItens(), recuperado.getItens());
        assertEquals(StatusOrcamento.APROVADO, recuperado.getStatus());
        assertEquals(
                new BigDecimal("91.00"),
                recuperado.calcularTotal());
    }
}
