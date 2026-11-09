package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataAccessResourceFailureException;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.CobrancaGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
class ReparoConcluidoListenerTest {
    private byte[] fixture() throws Exception { return getClass().getResourceAsStream("/contracts/reparo-concluido-v1.json").readAllBytes(); }
    @Test void deveConsumirContratoCompartilhadoSemDadosDePagamento() throws Exception {
        var g=mock(CobrancaGateway.class); var clock=Clock.fixed(Instant.parse("2026-10-20T16:00:00Z"),ZoneOffset.UTC);
        var listener=new ReparoConcluidoListener(new CobrancaService(g,clock)); listener.receber(fixture());
        var input=org.mockito.ArgumentCaptor.forClass(ConclusaoReparoInput.class);
        verify(g).criarOuRecuperar(input.capture(),eq(clock.instant()));
        assertEquals(input.getValue().eventId(),input.getValue().reparoId());
        assertEquals(Instant.parse("2026-10-20T15:00:00Z"),input.getValue().ocorridoEm());
    }
    @Test void deveEnviarEnvelopeOuCorrelacaoInvalidaParaDlq() throws Exception {
        var g=mock(CobrancaGateway.class); var listener=new ReparoConcluidoListener(new CobrancaService(g,Clock.systemUTC()));
        var original=new String(fixture(),StandardCharsets.UTF_8);

        for(var invalid:List.of("{","null",original.replace("ReparoConcluido","Outro"),original.replace("\"schemaVersion\": 1","\"schemaVersion\": 9")))
            assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.receber(invalid.getBytes(StandardCharsets.UTF_8)));
        var json=new tools.jackson.databind.json.JsonMapper();
        for(var field:List.of("eventId","reparoId","ordemId","orcamentoId","ocorridoEm")) {
            var tree=(tools.jackson.databind.node.ObjectNode)json.readTree(fixture()); tree.putNull(field);
            assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.receber(json.writeValueAsBytes(tree)));
        }
        for(var field:List.of("eventId","orcamentoId")) {
            var tree=(tools.jackson.databind.node.ObjectNode)json.readTree(fixture()); tree.put(field,java.util.UUID.randomUUID().toString());
            assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.receber(json.writeValueAsBytes(tree)));
        }
        verifyNoInteractions(g);
    }
    @Test void falhaTransitoriaDevePermitirRetryEConflitoDeveIrParaDlq() throws Exception {
        var g=mock(CobrancaGateway.class); var listener=new ReparoConcluidoListener(new CobrancaService(g,Clock.systemUTC()));
        when(g.criarOuRecuperar(any(),any())).thenThrow(new DataAccessResourceFailureException("Offline"))
                .thenThrow(new IllegalArgumentException("Orcamento nao aprovado"));
        assertThrows(DataAccessResourceFailureException.class,()->listener.receber(fixture()));
        assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.receber(fixture()));
    }
}
