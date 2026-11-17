package br.com.techchallenge.mecanica.billing.application.usecase;
import java.util.UUID;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
public class CriarCheckoutUseCase {
    private final CobrancaService cobrancas;
    private final CheckoutGateway gateway;
    private final ProvedorCheckout provedor;
    public CriarCheckoutUseCase(CobrancaService cobrancas, CheckoutGateway gateway, ProvedorCheckout provedor) {
        this.cobrancas=cobrancas; this.gateway=gateway; this.provedor=provedor;
    }
    public Checkout executar(UUID id) {
        var cobranca=cobrancas.buscar(id);
        if(cobranca.status()!=Cobranca.Status.PENDENTE)
            throw new RegraNegocioException("Cobrança não está pendente.");
        return gateway.buscar(id).orElseGet(()->gateway.salvarSeAusente(id,provedor.criar(cobranca)));
    }
}
