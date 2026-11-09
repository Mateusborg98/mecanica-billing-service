package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.net.URI;
import java.util.Arrays;
import org.springframework.core.env.Environment;

/** Production uses the provider endpoint; the isolated BDD profile may use only the named local simulator. */
public final class EndpointMercadoPago {
    private EndpointMercadoPago() { }
    public static URI resolver(Environment environment, String token) {
        var endpoint=environment.getProperty("mercado-pago.orders-url","https://api.mercadopago.com/v1/orders");
        if ("https://api.mercadopago.com/v1/orders".equals(endpoint)) return URI.create(endpoint);
        if (Arrays.asList(environment.getActiveProfiles()).contains("bdd") && "bdd-token-not-real".equals(token)
            && "http://provider:8090/v1/orders".equals(endpoint)) return URI.create(endpoint);
        throw new IllegalArgumentException("Endpoint de pagamento alternativo permitido somente no ambiente BDD isolado.");
    }
}
