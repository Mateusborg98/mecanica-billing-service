package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataAccessResourceFailureException;
import br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoDiagnosticoGateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
class DiagnosticoListenerTest {
    private byte[] fixture() throws Exception { return getClass().getResourceAsStream("/contracts/diagnostico-registrado-v1.json").readAllBytes(); }
    @Test void deveConsumirContratoComIdentificadorEstavel() throws Exception {
        var gateway = mock(OrcamentoDiagnosticoGateway.class);
        var listener = new DiagnosticoListener(new ReceberDiagnosticoUseCase(gateway));
        listener.receber(fixture()); listener.receber(fixture());
        var captor = org.mockito.ArgumentCaptor.forClass(Orcamento.class);
        verify(gateway, times(2)).inserirOuRecuperar(captor.capture());
        var a = captor.getAllValues().getFirst(); var b = captor.getAllValues().getLast();
        assertEquals(a.getId(), b.getId()); assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, a.getStatus());
        assertEquals(0, new java.math.BigDecimal("100.00").compareTo(a.calcularTotal()));
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), a.getOrdemId());
        assertEquals(Instant.parse("2026-10-16T12:00:00Z"), a.getCriadoEm());
    }
    @Test void deveRejeitarEnvelopeInvalidoSemAcessarPersistencia() throws Exception {
        var gateway = mock(OrcamentoDiagnosticoGateway.class); var listener = new DiagnosticoListener(new ReceberDiagnosticoUseCase(gateway));
        var original = new String(fixture(), StandardCharsets.UTF_8);
        for (var invalid : List.of("{", "null", original.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"),
                original.replace("DiagnosticoRegistrado", "OutroEvento"), original.replace("SERVICO", "OUTRO"),
                original.replace("\"quantidade\": 1", "\"quantidade\": 0"))) {
            assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.receber(invalid.getBytes(StandardCharsets.UTF_8)));
        }
        verifyNoInteractions(gateway);
    }
    @Test void devePermitirRetryDeFalhaTransitoriaMongo() throws Exception {
        var gateway = mock(OrcamentoDiagnosticoGateway.class);
        when(gateway.inserirOuRecuperar(any())).thenThrow(new DataAccessResourceFailureException("indisponível"));
        var listener = new DiagnosticoListener(new ReceberDiagnosticoUseCase(gateway));
        assertThrows(DataAccessResourceFailureException.class, () -> listener.receber(fixture()));
    }
    @Test void deveRejeitarConflitoDeConteudoNaDlq() throws Exception {
        var gateway = mock(OrcamentoDiagnosticoGateway.class);
        when(gateway.inserirOuRecuperar(any())).thenThrow(new IllegalArgumentException("conflito"));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> new DiagnosticoListener(new ReceberDiagnosticoUseCase(gateway)).receber(fixture()));
    }
}
