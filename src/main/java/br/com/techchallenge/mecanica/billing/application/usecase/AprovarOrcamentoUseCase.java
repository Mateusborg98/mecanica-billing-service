package br.com.techchallenge.mecanica.billing.application.usecase;

import java.util.Objects;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;

public class AprovarOrcamentoUseCase {

    private final BuscarOrcamentoPorIdUseCase buscarUseCase;
    private final OrcamentoGateway gateway;

    public AprovarOrcamentoUseCase(
            BuscarOrcamentoPorIdUseCase buscarUseCase,
            OrcamentoGateway gateway) {

        this.buscarUseCase = Objects.requireNonNull(buscarUseCase);
        this.gateway = Objects.requireNonNull(gateway);
    }

    public Orcamento executar(UUID id) {
        var orcamento = buscarUseCase.executar(id);

        orcamento.aprovar();
        gateway.salvar(orcamento);

        return orcamento;
    }
}
