package br.com.techchallenge.mecanica.billing.infrastructure.messaging;



import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitDecisaoPublisher implements java.util.function.Consumer<OrcamentoDecidido> {
    private final PublicadorRabbitConfirmado publicador;

    public RabbitDecisaoPublisher(RabbitTemplate rabbit) {
        this.publicador = new PublicadorRabbitConfirmado(rabbit);
    }
    public void accept(OrcamentoDecidido evento) {
        publicador.publicar(evento.eventId(), evento, RabbitTopology.DECISAO_ROUTE);
    }
    public void accept(DecisaoOrcamentoComValor evento) {
        publicador.publicar(evento.eventId(), evento, RabbitTopology.DECISAO_COM_VALOR_ROUTE);
    }
}
