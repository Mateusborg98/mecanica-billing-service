package br.com.techchallenge.mecanica.billing.presentation.controller;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.com.techchallenge.mecanica.billing.application.gateway.CobrancaGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.presentation.exception.GlobalExceptionHandler;
class CobrancaControllerTest {
    @Test void deveConsultarPendenteSemPermitirConfirmacaoManualDePagamento() throws Exception {
        var gateway=mock(CobrancaGateway.class); var id=UUID.randomUUID(); var now=Instant.now();
        var c=new Cobranca(id,UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("51.00"),"BRL",Cobranca.Status.PENDENTE,now,now);
        when(gateway.buscar(id)).thenReturn(Optional.of(c));
        var mvc=MockMvcBuilders.standaloneSetup(new CobrancaController(new CobrancaService(gateway,Clock.systemUTC())))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/cobrancas/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.moeda").value("BRL")).andExpect(jsonPath("$.valorTotal").value(51.0));
        mvc.perform(get("/cobrancas/"+UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/cobrancas/invalido")).andExpect(status().isBadRequest());
        mvc.perform(post("/cobrancas/"+id)).andExpect(status().isMethodNotAllowed());
    }
    @Test void cobrancaDeveExigirValorPositivoEMoedaSuportada() {
        var id=UUID.randomUUID(); var now=Instant.now();
        assertThrows(IllegalArgumentException.class,()->new Cobranca(id,id,id,BigDecimal.ZERO,"BRL",Cobranca.Status.PENDENTE,now,now));
        assertThrows(IllegalArgumentException.class,()->new Cobranca(id,id,id,BigDecimal.TEN,"USD",Cobranca.Status.PENDENTE,now,now));
    }
}
