package br.com.techchallenge.mecanica.billing.application.gateway;

import java.util.Optional;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;

public interface OrcamentoGateway {

    void salvar(Orcamento orcamento);

    Optional<Orcamento> buscarPorId(UUID id);
}
