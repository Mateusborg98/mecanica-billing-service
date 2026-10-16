package br.com.techchallenge.mecanica.billing.presentation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;

public record OrcamentoResponse(
        UUID id,
        UUID ordemId,
        StatusOrcamento status,
        Instant criadoEm,
        BigDecimal valorTotal,
        List<ItemResponse> itens) {

    public record ItemResponse(
            UUID referenciaId,
            TipoItemOrcamento tipo,
            String descricao,
            int quantidade,
            BigDecimal valorUnitario,
            BigDecimal subtotal) {
    }
}
