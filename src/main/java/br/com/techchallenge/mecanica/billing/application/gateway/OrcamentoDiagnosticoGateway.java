package br.com.techchallenge.mecanica.billing.application.gateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
public interface OrcamentoDiagnosticoGateway {
    Orcamento inserirOuRecuperar(Orcamento candidato, java.util.UUID reparoId);
}
