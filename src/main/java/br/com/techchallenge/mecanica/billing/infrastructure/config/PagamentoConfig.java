package br.com.techchallenge.mecanica.billing.infrastructure.config;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoPagamentoGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.infrastructure.payment.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.*;
@Configuration
@EnableScheduling
public class PagamentoConfig {
    @Bean public AssinaturaMercadoPago assinaturaMercadoPago(@Value("${mercado-pago.webhook.secret:}") String secret) {
        return new AssinaturaMercadoPago(secret);
    }
    @Bean public NotificacaoPagamentoGateway notificacaoPagamentoGateway(MongoTemplate mongo,Clock clock) {
        return new NotificacaoPagamentoMongo(mongo,clock);
    }
    @Bean public ConfirmarPagamentoUseCase confirmarPagamentoUseCase(MongoTemplate mongo,CobrancaService cobrancas,
            Clock clock,@Value("${mercado-pago.access-token:}") String token) {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
        return new ConfirmarPagamentoUseCase(new MercadoPagoConsulta(client,URI.create("https://api.mercadopago.com/v1/orders"),token),
            cobrancas,new CheckoutMongoGateway(mongo),new PagamentoMongoGateway(mongo),clock);
    }
    @Bean
    @ConditionalOnProperty(name="mercado-pago.webhook.enabled",havingValue="true")
    public PagamentoInboxWorker pagamentoInboxWorker(MongoTemplate mongo,ConfirmarPagamentoUseCase useCase,Clock clock) {
        return new PagamentoInboxWorker(mongo,useCase,clock);
    }
}
