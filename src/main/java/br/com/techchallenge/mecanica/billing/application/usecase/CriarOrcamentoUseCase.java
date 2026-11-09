package br.com.techchallenge.mecanica.billing.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import br.com.techchallenge.mecanica.billing.application.dto.CriarOrcamentoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;

public class CriarOrcamentoUseCase {

    private final OrcamentoGateway orcamentoGateway;
    private final Clock clock;

    public CriarOrcamentoUseCase(
            OrcamentoGateway orcamentoGateway,
            Clock clock) {

        this.orcamentoGateway = Objects.requireNonNull(orcamentoGateway);
        this.clock = Objects.requireNonNull(clock);
    }

    public Orcamento executar(CriarOrcamentoInput input) {
        if (input == null) {
            throw new RegraNegocioException(
                    "Dados para criação do orçamento obrigatórios");
        }

        var orcamento = new Orcamento(
                input.ordemId(),
                input.itens(),
                Instant.now(clock));

        orcamentoGateway.salvar(orcamento);

        return orcamento;
    }
}
