package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.techchallenge.mecanica.billing.application.gateway.CancelamentoReparoGateway;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataAccessResourceFailureException;
class ReparoCanceladoListenerTest {
    final CancelamentoReparoGateway gateway=mock(CancelamentoReparoGateway.class);
    final ReparoCanceladoListener listener=new ReparoCanceladoListener(gateway);
    byte[] fixture() throws Exception { return getClass().getResourceAsStream("/contracts/reparo-cancelado-v1.json").readAllBytes(); }
    @Test void aceitaContratoDoExecution() throws Exception { listener.receber(fixture());verify(gateway).registrar(any()); }
    @Test void rejeitaEnvelopeInvalido() throws Exception {
        for(String value:java.util.List.of("null","{}",new String(fixture(),java.nio.charset.StandardCharsets.UTF_8).replace("ReparoCancelado","Outro")))
            assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.receber(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        verifyNoInteractions(gateway);
    }
    @Test void falhaTransitoriaPermiteRetry() throws Exception {
        doThrow(new DataAccessResourceFailureException("offline")).when(gateway).registrar(any());
        assertThrows(DataAccessResourceFailureException.class,()->listener.receber(fixture()));
    }
}
