package br.com.techchallenge.mecanica.billing.application.usecase;
import java.time.Clock;
import java.util.UUID;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.CobrancaGateway;
import br.com.techchallenge.mecanica.billing.application.exception.CobrancaNaoEncontradaException;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
public class CobrancaService {
    private final CobrancaGateway gateway;
    private final Clock clock;
    public CobrancaService(CobrancaGateway gateway,Clock clock) { this.gateway=gateway; this.clock=clock; }
    public Cobranca receberConclusao(ConclusaoReparoInput input) { return gateway.criarOuRecuperar(input,clock.instant()); }
    public Cobranca buscar(UUID orcamentoId) { return gateway.buscar(orcamentoId).orElseThrow(CobrancaNaoEncontradaException::new); }
}
