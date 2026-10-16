package br.com.techchallenge.mecanica.billing.presentation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarOrcamentoRequest(
        @NotNull UUID ordemId,
        @NotEmpty List<@NotNull @Valid ItemRequest> itens) {

    public record ItemRequest(
            @NotNull UUID referenciaId,
            @NotNull TipoItemOrcamento tipo,
            @NotBlank String descricao,
            @Positive int quantidade,
            @NotNull @Positive BigDecimal valorUnitario) {
    }
}
