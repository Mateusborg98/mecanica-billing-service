package br.com.techchallenge.mecanica.billing.infrastructure.config;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.infrastructure.payment.MercadoPagoCheckout;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.CheckoutMongoGateway;
@Configuration
public class CheckoutConfig {
    @Bean public CriarCheckoutUseCase criarCheckoutUseCase(CobrancaService cobrancas, MongoTemplate mongo,
            @Value("${mercado-pago.access-token:}") String token) {
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
        return new CriarCheckoutUseCase(cobrancas,new CheckoutMongoGateway(mongo),
            new MercadoPagoCheckout(client,URI.create("https://api.mercadopago.com/v1/orders"),token));
    }
}
