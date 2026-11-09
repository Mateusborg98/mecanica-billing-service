package br.com.techchallenge.mecanica.billing.presentation.mapper;

import org.springframework.stereotype.Component;

import br.com.techchallenge.mecanica.billing.application.dto.CriarOrcamentoInput;
import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.presentation.dto.CriarOrcamentoRequest;
import br.com.techchallenge.mecanica.billing.presentation.dto.OrcamentoResponse;

@Component
public class OrcamentoPresentationMapper {

    public CriarOrcamentoInput toInput(CriarOrcamentoRequest request) {
        var itens = request.itens().stream()
                .map(item -> new ItemOrcamento(
                        item.referenciaId(),
                        item.tipo(),
                        item.descricao(),
                        item.quantidade(),
                        item.valorUnitario()))
                .toList();

        return new CriarOrcamentoInput(request.ordemId(), itens);
    }

    public OrcamentoResponse toResponse(Orcamento orcamento) {
        var itens = orcamento.getItens().stream()
                .map(item -> new OrcamentoResponse.ItemResponse(
                        item.referenciaId(),
                        item.tipo(),
                        item.descricao(),
                        item.quantidade(),
                        item.valorUnitario(),
                        item.calcularSubtotal()))
                .toList();

        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getOrdemId(),
                orcamento.getStatus(),
                orcamento.getCriadoEm(),
                orcamento.calcularTotal(),
                itens);
    }
}
