package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
import tools.jackson.databind.json.JsonMapper;
import br.com.techchallenge.mecanica.billing.application.dto.OrderPagamento;
import br.com.techchallenge.mecanica.billing.application.gateway.ConsultaPagamento;
import br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException;
public class MercadoPagoConsulta implements ConsultaPagamento {
    private final HttpClient client;
    private final URI base;
    private final String token;
    public MercadoPagoConsulta(HttpClient client,URI base,String token) { this.client=client; this.base=base; this.token=token; }
    public OrderPagamento consultar(String orderId) {
        if(token==null || token.isBlank() || orderId==null || !orderId.matches("[A-Za-z0-9]{1,80}"))
            throw new CheckoutIndisponivelException();
        try {
            var req=HttpRequest.newBuilder(URI.create(base.toString()+"/"+orderId)).timeout(Duration.ofSeconds(15))
                .header("Authorization","Bearer "+token).GET().build();
            var response=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new CheckoutIndisponivelException();
            var j=new JsonMapper().readTree(response.body());
            if(!orderId.equals(j.path("id").asString()) || !"online".equals(j.path("type").asString())
                    || j.path("status").asString("").isBlank() || j.path("currency").asString("").isBlank())
                throw new CheckoutIndisponivelException();
            return new OrderPagamento(orderId,UUID.fromString(j.path("external_reference").asString()),
                j.path("status").asString(),j.path("status_detail").asString(),j.path("currency").asString(),
                new BigDecimal(j.path("total_amount").asString()),new BigDecimal(j.path("total_paid_amount").asString()),
                Instant.parse(j.path("last_updated_date").asString()));
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new CheckoutIndisponivelException(); }
        catch(Exception e) { throw new CheckoutIndisponivelException(); }
    }
}
