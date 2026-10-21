package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.math.RoundingMode;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException;
import br.com.techchallenge.mecanica.billing.application.gateway.ProvedorCheckout;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
public class MercadoPagoCheckout implements ProvedorCheckout {
    private final HttpClient client;
    private final URI endpoint;
    private final String token;
    private final JsonMapper json=new JsonMapper();
    public MercadoPagoCheckout(HttpClient client, URI endpoint, String token) {
        this.client=client; this.endpoint=endpoint; this.token=token;
    }
    public Checkout criar(Cobranca c) {
        if(token==null || token.isBlank()) throw new CheckoutIndisponivelException();
        try {
            var valor=c.valorTotal().setScale(2,RoundingMode.UNNECESSARY).toPlainString();
            // Immutable charge snapshot + stable key allow retry after an uncertain HTTP/DB failure.
            var body=Map.of("type","online","processing_mode","manual","total_amount",valor,
                "external_reference",c.id().toString(),"items",List.of(Map.of(
                    "title","Reparo da oficina", "quantity",1,"unit_price",valor)));
            var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
                .header("Authorization","Bearer "+token).header("Content-Type","application/json")
                .header("X-Idempotency-Key",c.id().toString())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()<200 || response.statusCode()>=300) throw new CheckoutIndisponivelException();
            var result=json.readTree(response.body());
            var id=result.path("id").asText("");
            var url=URI.create(result.path("checkout_url").asText(""));
            if(id.isBlank() || !"https".equals(url.getScheme()) || !"www.mercadopago.com.br".equals(url.getHost())
                || url.getUserInfo()!=null || (url.getPort()!=-1 && url.getPort()!=443)
                || !c.id().toString().equals(result.path("external_reference").asText())
                || !"BRL".equals(result.path("currency").asText())
                || c.valorTotal().compareTo(new java.math.BigDecimal(result.path("total_amount").asText()))!=0)
                throw new CheckoutIndisponivelException();
            return new Checkout(id,url.toString());
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw new CheckoutIndisponivelException();
        } catch(Exception e) {
            // Provider response and authorization must never leak into API errors or logs.
            throw new CheckoutIndisponivelException();
        }
    }
}
