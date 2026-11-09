package br.com.techchallenge.mecanica.billing.presentation.controller;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoPagamentoGateway;
import br.com.techchallenge.mecanica.billing.infrastructure.payment.*;
import br.com.techchallenge.mecanica.billing.presentation.exception.GlobalExceptionHandler;
class MercadoPagoWebhookControllerTest {
    static final String SIGNATURE="ts=1792771200000,v1=e590347e7653fc8de4c52de82ff099308da8f9fdc0410794d52e8a908735882a";
    final NotificacaoPagamentoGateway inbox=mock(NotificacaoPagamentoGateway.class);
    MockMvc mvc;
    @BeforeEach void setup() { mvc=MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(
        new AssinaturaMercadoPago("test-webhook-secret"),inbox)).setControllerAdvice(new GlobalExceptionHandler()).build(); }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder notification() {
        return post("/webhooks/mercado-pago").param("data.id","ORDTEST123").param("type","order")
            .header("x-request-id","req-123").header("x-signature",SIGNATURE);
    }
    @Test void autenticaQueryIgnoraStatusEValoresDoCorpo() throws Exception {
        mvc.perform(notification().contentType("application/json").content("{\"data\":{\"id\":\"FAKE\",\"status\":\"approved\",\"total_amount\":999999}}"))
            .andExpect(status().isOk()); verify(inbox).registrar("ORDTEST123");
    }
    @Test void assinaturaInvalidaOuDuplicadaNaoPersiste() throws Exception {
        mvc.perform(post("/webhooks/mercado-pago").param("data.id","ORDTEST123").param("type","order"))
            .andExpect(status().isUnauthorized());
        mvc.perform(notification().param("data.id","OTHER")).andExpect(status().isUnauthorized());
        mvc.perform(notification().header("x-signature",SIGNATURE)).andExpect(status().isUnauthorized());
        mvc.perform(post("/webhooks/mercado-pago").param("data.id","ORDOTHER").param("type","order")
            .header("x-request-id","req-123").header("x-signature",SIGNATURE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(inbox);
    }
    @Test void naoConfirmaRecebimentoQuandoBancoFalha() throws Exception {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("private-uri-password"))
            .when(inbox).registrar("ORDTEST123");
        mvc.perform(notification()).andExpect(status().isServiceUnavailable())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-uri"))));
    }
    @Test void ignoraOutroTipoAutenticado() throws Exception {
        mvc.perform(post("/webhooks/mercado-pago").param("data.id","ORDTEST123").param("type","payment")
            .header("x-request-id","req-123").header("x-signature",SIGNATURE)).andExpect(status().isOk());
        verifyNoInteractions(inbox);
    }
    @Test void exigeSecretSemImpedirStartupDoBilling() throws Exception {
        var semSecret=MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(new AssinaturaMercadoPago(""),inbox))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        semSecret.perform(notification()).andExpect(status().isServiceUnavailable()); verifyNoInteractions(inbox);
    }
    @Test void rejeitaManifestoMalformado() {
        var verifier=new AssinaturaMercadoPago("test-webhook-secret");
        for(String s:java.util.List.of("", "v1=abc", "ts=abc,v1="+"0".repeat(64), "ts=1,ts=2,v1="+"0".repeat(64),
                "ts=1,v1="+"g".repeat(64),"x".repeat(1025),"broken",SIGNATURE.replace("1792771200000","1792771200001")))
            assertThrows(AssinaturaWebhookInvalidaException.class,()->verifier.validar("ORDTEST123","req-123",s));
        assertThrows(AssinaturaWebhookInvalidaException.class,()->verifier.validar("../orders","req-123",SIGNATURE));
        assertThrows(AssinaturaWebhookInvalidaException.class,()->verifier.validar("ORDTEST123","req;injected",SIGNATURE));
        assertDoesNotThrow(()->verifier.validar("ORDTEST123","req-123",SIGNATURE));
    }
}
