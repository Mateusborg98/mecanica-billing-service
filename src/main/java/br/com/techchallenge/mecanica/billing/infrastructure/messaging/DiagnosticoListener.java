package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import java.util.Comparator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import tools.jackson.databind.json.JsonMapper;
import br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
public class DiagnosticoListener {
    private final ReceberDiagnosticoUseCase useCase;
    private final JsonMapper json = new JsonMapper();
    public DiagnosticoListener(ReceberDiagnosticoUseCase useCase) { this.useCase = useCase; }
    @RabbitListener(queues=RabbitTopology.QUEUE)
    public void receber(byte[] body) {
        try {
            var e = json.readValue(body, DiagnosticoRegistrado.class);
            if (e == null || e.schemaVersion() != 1 || !"DiagnosticoRegistrado".equals(e.eventType())
                    || e.reparoId() == null || !e.reparoId().equals(e.eventId()) || e.itens() == null || e.itens().isEmpty())
                throw new IllegalArgumentException("Envelope de diagnóstico inválido.");
            var itens = e.itens().stream().map(i -> {
                if (i == null || i.tipo() == null) throw new IllegalArgumentException("Item inválido.");
                return new ItemOrcamento(i.referenciaId(), TipoItemOrcamento.valueOf(i.tipo()), i.descricao(), i.quantidade(), i.valorUnitario());
            }).sorted(Comparator.comparing((ItemOrcamento i) -> i.tipo().name()).thenComparing(i -> i.referenciaId().toString())).toList();
            if (itens.stream().map(i -> i.tipo() + ":" + i.referenciaId()).distinct().count() != itens.size())
                throw new IllegalArgumentException("Item duplicado.");
            useCase.executar(e.reparoId(), e.ordemId(), e.ocorridoEm(), itens);
        } catch (tools.jackson.core.JacksonException | IllegalArgumentException | RegraNegocioException exception) {
            throw new AmqpRejectAndDontRequeueException("Diagnóstico inválido ou incompatível; verificar DLQ.", exception);
        }
    }
}
