package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException;
class MercadoPagoConsultaTest {
    HttpServer server; String body; int status=200;
    final UUID id=UUID.randomUUID();
    final AtomicReference<String> auth=new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        body="{\"id\":\"ORDTEST\",\"type\":\"online\",\"external_reference\":\""+id+"\",\"status\":\"processed\","
            +"\"status_detail\":\"accredited\",\"currency\":\"BRL\",\"total_amount\":\"51.00\",\"total_paid_amount\":\"51.00\","
            +"\"last_updated_date\":\"2026-10-23T12:00:00Z\"}";
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/orders/ORDTEST",exchange->{
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            var bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start();
    }
    @AfterEach void stop() { if(server!=null) server.stop(0); Thread.interrupted(); }
    URI base() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/orders"); }
    MercadoPagoConsulta adapter() { return new MercadoPagoConsulta(HttpClient.newHttpClient(),base(),"fake-token"); }
    @Test void consultaDiretamenteComTokenESemConfiarNoWebhook() {
        var result=adapter().consultar("ORDTEST"); assertTrue(result.creditada());
        assertEquals(id,result.cobrancaId()); assertEquals("Bearer fake-token",auth.get());
        assertEquals(0,new java.math.BigDecimal("51").compareTo(result.pago()));
    }
    @Test void naoVazaRespostaDeErroERejeitaDadosInvalidos() {
        String valid=body;
        for(String invalid:java.util.List.of("{}","broken",valid.replace("ORDTEST","OTHER"),valid.replace("online","point"),
                valid.replace(id.toString(),"invalid-ref"),valid.replace("51.00","NaN"),valid.replace("2026-10-23T12:00:00Z","bad-date"))) {
            body=invalid; assertThrows(CheckoutIndisponivelException.class,()->adapter().consultar("ORDTEST"));
        }
        status=401; body="private-token";
        var error=assertThrows(CheckoutIndisponivelException.class,()->adapter().consultar("ORDTEST"));
        assertFalse(error.toString().contains("private-token")); assertNull(error.getCause());
    }
    @Test void bloqueiaIdInjetadoETokenAusenteAntesDaRede() {
        assertThrows(CheckoutIndisponivelException.class,()->adapter().consultar("../users"));
        assertThrows(CheckoutIndisponivelException.class,()->new MercadoPagoConsulta(HttpClient.newHttpClient(),base(),"").consultar("ORDTEST"));
        assertNull(auth.get());
    }
    @Test void interrupcaoPreservaFlag() throws Exception {
        var client=mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        assertThrows(CheckoutIndisponivelException.class,()->new MercadoPagoConsulta(client,base(),"fake").consultar("ORDTEST"));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
