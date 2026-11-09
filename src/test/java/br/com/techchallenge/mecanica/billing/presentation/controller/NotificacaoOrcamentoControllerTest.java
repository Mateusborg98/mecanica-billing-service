package br.com.techchallenge.mecanica.billing.presentation.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoOrcamentoGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.EnviarOrcamentoUseCase;
class NotificacaoOrcamentoControllerTest {
    @Test void solicitaEnvioConsultaSituacaoERejeitaEmailInvalido() throws Exception {
        var usecase=mock(EnviarOrcamentoUseCase.class);var gateway=mock(NotificacaoOrcamentoGateway.class);var id=UUID.randomUUID();
        var resultado=new NotificacaoOrcamentoGateway.Situacao(id,"PENDENTE",0);
        when(usecase.executar(id,"cliente@example.test")).thenReturn(resultado);when(gateway.buscar(id)).thenReturn(resultado);
        var mvc=MockMvcBuilders.standaloneSetup(new NotificacaoOrcamentoController(usecase,gateway)).build();
        mvc.perform(post("/orcamentos/"+id+"/notificacao").contentType(MediaType.APPLICATION_JSON).content("{\"destinatario\":\"cliente@example.test\"}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(get("/orcamentos/"+id+"/notificacao")).andExpect(status().isOk());
        mvc.perform(post("/orcamentos/"+id+"/notificacao").contentType(MediaType.APPLICATION_JSON).content("{\"destinatario\":\"invalido\"}"))
            .andExpect(status().isBadRequest());verify(usecase,times(1)).executar(id,"cliente@example.test");
    }
}
