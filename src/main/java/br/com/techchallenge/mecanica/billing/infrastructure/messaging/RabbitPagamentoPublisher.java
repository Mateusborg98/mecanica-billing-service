package br.com.techchallenge.mecanica.billing.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitPagamentoPublisher implements java.util.function.Consumer<PagamentoConfirmado> {
    private final PublicadorRabbitConfirmado publicador;

    public RabbitPagamentoPublisher(RabbitTemplate rabbit) {
        this.publicador = new PublicadorRabbitConfirmado(rabbit);
    }
    public void accept(PagamentoConfirmado evento) {
        var eventId = evento.eventId();
        publicador.publicar(eventId, evento, RabbitTopology.PAGAMENTO_ROUTE);
    }
}
