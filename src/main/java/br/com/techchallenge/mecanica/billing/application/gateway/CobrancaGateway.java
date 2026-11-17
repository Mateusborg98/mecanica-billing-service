package br.com.techchallenge.mecanica.billing.application.gateway;
import java.time.Instant;
import java.util.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
public interface CobrancaGateway {
    Cobranca criarOuRecuperar(ConclusaoReparoInput input, Instant registradaEm);
    Optional<Cobranca> buscar(UUID orcamentoId);
}
