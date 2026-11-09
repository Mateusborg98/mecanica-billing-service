package br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper;

import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.ItemOrcamentoDocument;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;

public class OrcamentoPersistenceMapper {

    public OrcamentoDocument toDocument(Orcamento orcamento) {
        var itens = orcamento.getItens().stream()
                .map(this::toItemDocument)
                .toList();

        return new OrcamentoDocument(
                orcamento.getId().toString(),
                orcamento.getOrdemId().toString(),
                itens,
                orcamento.getCriadoEm(),
                orcamento.getStatus().name());
    }

    public Orcamento toDomain(OrcamentoDocument document) {
        var itens = document.itens().stream()
                .map(this::toItemDomain)
                .toList();

        return Orcamento.reconstituir(
                UUID.fromString(document.id()),
                UUID.fromString(document.ordemId()),
                itens,
                document.criadoEm(),
                StatusOrcamento.valueOf(document.status()));
    }

    private ItemOrcamentoDocument toItemDocument(ItemOrcamento item) {
        return new ItemOrcamentoDocument(
                item.referenciaId().toString(),
                item.tipo().name(),
                item.descricao(),
                item.quantidade(),
                item.valorUnitario());
    }

    private ItemOrcamento toItemDomain(ItemOrcamentoDocument document) {
        return new ItemOrcamento(
                UUID.fromString(document.referenciaId()),
                TipoItemOrcamento.valueOf(document.tipo()),
                document.descricao(),
                document.quantidade(),
                document.valorUnitario());
    }
}
