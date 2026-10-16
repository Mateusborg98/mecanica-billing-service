package br.com.techchallenge.mecanica.billing.application.dto;

import java.util.List;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;

public record CriarOrcamentoInput(
        UUID ordemId,
        List<ItemOrcamento> itens) {
}
