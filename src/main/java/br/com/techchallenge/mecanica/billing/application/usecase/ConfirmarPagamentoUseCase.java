package br.com.techchallenge.mecanica.billing.application.usecase;
import java.time.Clock;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.application.exception.*;
public class ConfirmarPagamentoUseCase {
    private final ConsultaPagamento provedor;
    private final CobrancaService cobrancas;
    private final CheckoutGateway checkouts;
    private final PagamentoGateway pagamentos;
    private final Clock clock;
    public ConfirmarPagamentoUseCase(ConsultaPagamento provedor,CobrancaService cobrancas,
            CheckoutGateway checkouts,PagamentoGateway pagamentos,Clock clock) {
        this.provedor=provedor; this.cobrancas=cobrancas; this.checkouts=checkouts; this.pagamentos=pagamentos; this.clock=clock;
    }
    public void executar(String orderId) {
        var order=provedor.consultar(orderId);
        if(!orderId.equals(order.orderId())) throw new PagamentoDivergenteException();
        var cobranca=cobrancas.buscar(order.cobrancaId());
        var checkout=checkouts.buscar(order.cobrancaId()).orElseThrow(CheckoutIndisponivelException::new);
        if(!orderId.equals(checkout.orderId()) || !cobranca.moeda().equals(order.moeda())
                || cobranca.valorTotal().compareTo(order.total())!=0)
            throw new PagamentoDivergenteException();
        if(!order.creditada()) return;
        if(cobranca.valorTotal().compareTo(order.pago())!=0) throw new PagamentoDivergenteException();
        pagamentos.confirmar(order,clock.instant());
    }
}
