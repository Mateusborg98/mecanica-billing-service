package br.com.techchallenge.mecanica.billing.domain.orcamento;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

public record ItemOrcamento(
        UUID referenciaId,
        TipoItemOrcamento tipo,
        String descricao,
        int quantidade,
        BigDecimal valorUnitario) {

    public ItemOrcamento {
        if (referenciaId == null) {
            throw new RegraNegocioException(
                    "Referência do item obrigatória");
        }

        if (tipo == null) {
            throw new RegraNegocioException(
                    "Tipo do item obrigatório");
        }

        if (descricao == null || descricao.isBlank()) {
            throw new RegraNegocioException(
                    "Descrição do item obrigatória");
        }

        if (quantidade <= 0) {
            throw new RegraNegocioException(
                    "Quantidade deve ser maior que zero");
        }

        if (valorUnitario == null
                || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException(
                    "Valor unitário deve ser maior que zero");
        }

        descricao = descricao.strip();
    }

    public BigDecimal calcularSubtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
