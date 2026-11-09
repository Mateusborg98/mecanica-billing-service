package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.JacksonException;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
public class ReparoConcluidoListener {
    private final CobrancaService service;
    private final JsonMapper json=new JsonMapper();
    public ReparoConcluidoListener(CobrancaService service) { this.service=service; }
    @RabbitListener(queues=RabbitTopology.CONCLUSAO_QUEUE)
    public void receber(byte[] body) {
        try {
            var e=json.readValue(body,ReparoConcluido.class);
            if(e==null || e.schemaVersion()!=1 || !"ReparoConcluido".equals(e.eventType()))
                throw new IllegalArgumentException("Envelope de conclusão inválido.");
            service.receberConclusao(new ConclusaoReparoInput(e.eventId(),e.reparoId(),e.ordemId(),e.orcamentoId(),e.ocorridoEm()));
        } catch(JacksonException | IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException("Conclusão inválida ou incompatível.",exception);
        }
    }
}
