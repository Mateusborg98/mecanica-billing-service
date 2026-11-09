package br.com.techchallenge.mecanica.billing.presentation.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.application.exception.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.presentation.exception.GlobalExceptionHandler;
class CheckoutControllerTest {
    @Test void criaReutilizaEExigeCobrancaConcluida() throws Exception {
        var cg=mock(CobrancaGateway.class); var checkout=mock(CheckoutGateway.class); var provider=mock(ProvedorCheckout.class);
        var id=UUID.randomUUID(); var now=Instant.now();
        var charge=new Cobranca(id,id,id,new BigDecimal("51.00"),"BRL",Cobranca.Status.PENDENTE,now,now);
        var result=new Checkout("ORDTEST","https://www.mercadopago.com.br/checkout");
        when(cg.buscar(id)).thenReturn(Optional.of(charge));
        when(provider.criar(charge)).thenReturn(result); when(checkout.salvarSeAusente(id,result)).thenReturn(result);
        var usecase=new CriarCheckoutUseCase(new CobrancaService(cg,Clock.systemUTC()),checkout,provider);
        var mvc=MockMvcBuilders.standaloneSetup(new CheckoutController(usecase)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/cobrancas/"+id+"/checkout")).andExpect(status().isOk()).andExpect(jsonPath("$.orderId").value("ORDTEST"));
        when(checkout.buscar(id)).thenReturn(Optional.of(result));
        mvc.perform(post("/cobrancas/"+id+"/checkout")).andExpect(status().isOk()); verify(provider,times(1)).criar(charge);
        mvc.perform(post("/cobrancas/"+UUID.randomUUID()+"/checkout")).andExpect(status().isNotFound());
        mvc.perform(post("/cobrancas/invalido/checkout")).andExpect(status().isBadRequest());
        when(checkout.buscar(id)).thenReturn(Optional.empty()); when(provider.criar(charge)).thenThrow(new CheckoutIndisponivelException());
        mvc.perform(post("/cobrancas/"+id+"/checkout")).andExpect(status().isServiceUnavailable());
        verify(checkout,times(1)).salvarSeAusente(id,result);
    }
}
