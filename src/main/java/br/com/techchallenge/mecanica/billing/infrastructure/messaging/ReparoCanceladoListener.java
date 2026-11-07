package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tools.jackson.databind.json.JsonMapper;
import br.com.techchallenge.mecanica.billing.application.dto.CancelamentoReparoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.CancelamentoReparoGateway;
public class ReparoCanceladoListener {
    private final CancelamentoReparoGateway gateway;
    private final JsonMapper json=new JsonMapper();
    public ReparoCanceladoListener(CancelamentoReparoGateway gateway) { this.gateway=gateway; }
    @RabbitListener(queues="billing.reparo-cancelado.v1")
    public void receber(byte[] body) {
        try {
            var e=json.readValue(body,CancelamentoReparoInput.class);
            if(e==null) throw new IllegalArgumentException("Evento ausente");
            gateway.registrar(e);
        } catch(tools.jackson.core.JacksonException | IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException("Cancelamento inválido ou conflitante",exception);
        }
    }
}
