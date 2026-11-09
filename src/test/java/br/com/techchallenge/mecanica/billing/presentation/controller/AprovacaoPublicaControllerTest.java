package br.com.techchallenge.mecanica.billing.presentation.controller;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.presentation.exception.GlobalExceptionHandler;
class AprovacaoPublicaControllerTest {
    BuscarOrcamentoPorIdUseCase buscar=mock(BuscarOrcamentoPorIdUseCase.class);
    AprovarOrcamentoUseCase aprovar=mock(AprovarOrcamentoUseCase.class);
    RecusarOrcamentoUseCase recusar=mock(RecusarOrcamentoUseCase.class);
    UUID id=UUID.randomUUID();MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.standaloneSetup(new AprovacaoPublicaController(buscar,aprovar,recusar)).setControllerAdvice(new GlobalExceptionHandler()).build();}
    void preparar(StatusOrcamento status){when(buscar.executar(id)).thenReturn(Orcamento.reconstituir(id,UUID.randomUUID(),
        List.of(new ItemOrcamento(UUID.randomUUID(),TipoItemOrcamento.SERVICO,"<script>alert(1)</script>",2,new BigDecimal("25.50"))),Instant.now(),status));}
    @Test void paginaEscapaItensEMostraValorSemAlterarEstado() throws Exception {
        preparar(StatusOrcamento.AGUARDANDO_APROVACAO);
        var result=mvc.perform(get("/orcamentos/"+id+"/aprovacao")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andReturn();
        String html=result.getResponse().getContentAsString();assertTrue(html.contains("&lt;script&gt;"));assertFalse(html.contains("<script>"));
        assertTrue(html.contains("51.00"));assertTrue(html.contains("method='post'"));verifyNoInteractions(aprovar,recusar);
    }
    @Test void orcamentoDecididoNaoOfereceOutraDecisao() throws Exception {
        for(var status:List.of(StatusOrcamento.APROVADO,StatusOrcamento.RECUSADO,StatusOrcamento.CANCELADO)){
            preparar(status);String html=mvc.perform(get("/orcamentos/"+id+"/aprovacao")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertFalse(html.contains("<form"));
        }
    }
    @Test void formularioUsaOsMesmosCasosDeUsoERedireciona() throws Exception {
        for(String decisao:List.of("APROVAR","RECUSAR"))mvc.perform(post("/orcamentos/"+id+"/aprovacao").param("decisao",decisao))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","/orcamentos/"+id+"/aprovacao"));
        verify(aprovar).executar(id);verify(recusar).executar(id);
        mvc.perform(post("/orcamentos/"+id+"/aprovacao").param("decisao","INVALIDA")).andExpect(status().isBadRequest());
    }
}
