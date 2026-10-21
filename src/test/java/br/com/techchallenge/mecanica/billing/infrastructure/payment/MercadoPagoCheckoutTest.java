package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException;
class MercadoPagoCheckoutTest {
    HttpServer server;
    final UUID id=UUID.randomUUID();
    final Cobranca charge=new Cobranca(id,id,id,new BigDecimal("51.00"),"BRL",Cobranca.Status.PENDENTE,Instant.now(),Instant.now());
    final AtomicReference<String> body=new AtomicReference<>(), key=new AtomicReference<>(), auth=new AtomicReference<>();
    String response; int status=201;
    @BeforeEach void setup() throws Exception {
        response="{\"id\":\"ORDTEST\",\"checkout_url\":\"https://www.mercadopago.com.br/checkout/v1/redirect?order_id=ORDTEST\","
            +"\"external_reference\":\""+id+"\",\"currency\":\"BRL\",\"total_amount\":\"51.00\"}";
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/orders", exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes=response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start();
    }
    @AfterEach void stop() { server.stop(0); Thread.interrupted(); }
    URI endpoint() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/orders"); }
    MercadoPagoCheckout provider() { return new MercadoPagoCheckout(HttpClient.newHttpClient(),endpoint(),"TEST-fake"); }
    @Test void enviaValorDaCobrancaERepeteMesmaChave() {
        var result=provider().criar(charge);
        assertEquals("ORDTEST",result.orderId()); assertEquals("Bearer TEST-fake",auth.get());
        var json=new JsonMapper().readTree(body.get());
        assertEquals("51.00",json.path("total_amount").asText());
        assertEquals("manual",json.path("processing_mode").asText());
        assertEquals("online",json.path("type").asText());
        assertEquals("51.00",json.path("items").get(0).path("unit_price").asText());
        assertEquals(id.toString(),json.path("external_reference").asText());
        String first=body.get(); provider().criar(charge);
        assertEquals(id.toString(),key.get()); assertEquals(first,body.get());
    }
    @Test void erroDoProvedorNaoVazaCredenciaisOuResposta() {
        status=401; response="private provider details TEST-fake";
        var error=assertThrows(CheckoutIndisponivelException.class,()->provider().criar(charge));
        assertFalse(error.toString().contains("TEST-fake")); assertNull(error.getCause());
    }
    @Test void rejeitaRespostaIncompletaEMoedaValorReferenciaOuDominioDiferentes() {
        String valid=response;
        for(String invalid:List.of("{}","not-json",valid.replace("BRL","USD"),valid.replace("51.00","50.00"),
            valid.replace(id.toString(),UUID.randomUUID().toString()),valid.replace("www.mercadopago.com.br","evil.example"),
            valid.replace("https:","http:"),valid.replace("ORDTEST",""))) {
            response=invalid; assertThrows(CheckoutIndisponivelException.class,()->provider().criar(charge));
        }
    }
    @Test void semTokenNaoChamaRede() {
        assertThrows(CheckoutIndisponivelException.class,()->new MercadoPagoCheckout(HttpClient.newHttpClient(),endpoint(),"").criar(charge));
        assertNull(body.get());
    }
    @Test void falhaDeRedePermiteRepetirSemTrocarChave() throws Exception {
        var client=mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new java.io.IOException("network"));
        assertThrows(CheckoutIndisponivelException.class,()->new MercadoPagoCheckout(client,endpoint(),"fake").criar(charge));
    }
    @Test void interrupcaoPreservaFlag() throws Exception {
        var client=mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        assertThrows(CheckoutIndisponivelException.class,()->new MercadoPagoCheckout(client,endpoint(),"fake").criar(charge));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
