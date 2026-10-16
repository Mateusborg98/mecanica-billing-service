package br.com.techchallenge.mecanica.billing.application.usecase;

import java.util.Objects;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;

public class BuscarOrcamentoPorIdUseCase {

    private final OrcamentoGateway orcamentoGateway;

    public BuscarOrcamentoPorIdUseCase(OrcamentoGateway orcamentoGateway) {
        this.orcamentoGateway = Objects.requireNonNull(orcamentoGateway);
    }

    public Orcamento executar(UUID id) {
        if (id == null) {
            throw new RegraNegocioException(
                    "Identificador do orçamento obrigatório");
        }

        return orcamentoGateway.buscarPorId(id)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
    }
}
