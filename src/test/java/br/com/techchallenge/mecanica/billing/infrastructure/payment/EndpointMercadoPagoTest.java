package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;
class EndpointMercadoPagoTest {
    @Test void producaoUsaSomenteProvedorOficial() {
        assertEquals("https://api.mercadopago.com/v1/orders",EndpointMercadoPago.resolver(new MockEnvironment(),"real-token-placeholder").toString());
        var env=new MockEnvironment().withProperty("mercado-pago.orders-url","http://provider:8090/v1/orders");
        assertThrows(IllegalArgumentException.class,()->EndpointMercadoPago.resolver(env,"bdd-token-not-real"));
        env.setActiveProfiles("bdd");
        assertThrows(IllegalArgumentException.class,()->EndpointMercadoPago.resolver(env,"real-token-placeholder"));
        assertEquals("http://provider:8090/v1/orders",EndpointMercadoPago.resolver(env,"bdd-token-not-real").toString());
        env.setProperty("mercado-pago.orders-url","https://example.com/v1/orders");
        assertThrows(IllegalArgumentException.class,()->EndpointMercadoPago.resolver(env,"bdd-token-not-real"));
    }
}
