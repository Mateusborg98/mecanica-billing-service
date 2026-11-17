package br.com.techchallenge.mecanica.billing.application.usecase;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import br.com.techchallenge.mecanica.billing.application.dto.*;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.application.exception.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
class ConfirmarPagamentoUseCaseTest {
    final ConsultaPagamento provider=mock(ConsultaPagamento.class);
    final CobrancaService charges=mock(CobrancaService.class);
    final CheckoutGateway checkouts=mock(CheckoutGateway.class);
    final PagamentoGateway payments=mock(PagamentoGateway.class);
    final UUID id=UUID.randomUUID(); final Instant now=Instant.parse("2026-10-23T12:00:00Z");
    ConfirmarPagamentoUseCase usecase;
    OrderPagamento order(String oid,UUID ref,String status,String detail,String currency,String total,String paid) {
        return new OrderPagamento(oid,ref,status,detail,currency,new BigDecimal(total),new BigDecimal(paid),now);
    }
    OrderPagamento valid() { return order("ORD1",id,"processed","accredited","BRL","51.00","51.00"); }
    @BeforeEach void setup() {
        usecase=new ConfirmarPagamentoUseCase(provider,charges,checkouts,payments,Clock.fixed(now,ZoneOffset.UTC));
        when(charges.buscar(id)).thenReturn(new Cobranca(id,id,id,new BigDecimal("51.00"),"BRL",Cobranca.Status.PENDENTE,now,now));
        when(checkouts.buscar(id)).thenReturn(Optional.of(new Checkout("ORD1","url")));
        when(provider.consultar("ORD1")).thenReturn(valid());
    }
    @Test void confirmaSomenteValorIntegralCreditado() {
        usecase.executar("ORD1"); verify(payments).confirmar(valid(),now);
    }
    @Test void naoConfirmaPendenteAutorizadoRecusadoOuEstornado() {
        for(String[] state:List.of(new String[]{"created","created"},new String[]{"processing","in_process"},
                new String[]{"action_required","waiting_capture"},new String[]{"failed","high_risk"},
                new String[]{"refunded","refunded"},new String[]{"processed","partially_refunded"})) {
            when(provider.consultar("ORD1")).thenReturn(order("ORD1",id,state[0],state[1],"BRL","51.00","51.00"));
            usecase.executar("ORD1");
        } verifyNoInteractions(payments);
    }
    @Test void rejeitaDivergenciaDeOrderMoedaValorOuPagamentoParcial() {
        for(var invalid:List.of(order("ORD2",id,"processed","accredited","BRL","51","51"),
                order("ORD1",id,"processed","accredited","USD","51","51"),
                order("ORD1",id,"processed","accredited","BRL","52","52"),
                order("ORD1",id,"processed","accredited","BRL","51","50"))) {
            when(provider.consultar("ORD1")).thenReturn(invalid);
            assertThrows(PagamentoDivergenteException.class,()->usecase.executar("ORD1"));
        } verifyNoInteractions(payments);
    }
    @Test void aguardaCheckoutPersistidoERejeitaOutroCheckout() {
        when(checkouts.buscar(id)).thenReturn(Optional.empty());
        assertThrows(CheckoutIndisponivelException.class,()->usecase.executar("ORD1"));
        when(checkouts.buscar(id)).thenReturn(Optional.of(new Checkout("ORD2","url")));
        assertThrows(PagamentoDivergenteException.class,()->usecase.executar("ORD1")); verifyNoInteractions(payments);
    }
    @Test void cobrancaAusenteOuProvedorIndisponivelNaoConfirma() {
        when(charges.buscar(id)).thenThrow(new CobrancaNaoEncontradaException());
        assertThrows(CobrancaNaoEncontradaException.class,()->usecase.executar("ORD1"));
        when(provider.consultar("ORD1")).thenThrow(new CheckoutIndisponivelException());
        assertThrows(CheckoutIndisponivelException.class,()->usecase.executar("ORD1")); verifyNoInteractions(payments);
    }
}
