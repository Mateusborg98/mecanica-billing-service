package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoPagamentoGateway;
import br.com.techchallenge.mecanica.billing.infrastructure.payment.*;
@RestController
@ConditionalOnProperty(name="mercado-pago.webhook.enabled",havingValue="true")
public class MercadoPagoWebhookController {
    private final AssinaturaMercadoPago assinatura;
    private final NotificacaoPagamentoGateway inbox;
    public MercadoPagoWebhookController(AssinaturaMercadoPago assinatura,NotificacaoPagamentoGateway inbox) {
        this.assinatura=assinatura; this.inbox=inbox;
    }
    @PostMapping("/webhooks/mercado-pago")
    public ResponseEntity<Void> receber(@RequestParam MultiValueMap<String,String> query,@RequestHeader HttpHeaders headers) {
        var orderId=unico(query.get("data.id"));
        assinatura.validar(orderId,unico(headers.get("x-request-id")),unico(headers.get("x-signature")));
        if(!"order".equals(unico(query.get("type")))) return ResponseEntity.ok().build();
        // Payload fields are deliberately not trusted. Persist before acknowledging delivery.
        inbox.registrar(orderId);
        return ResponseEntity.ok().build();
    }
    private String unico(List<String> values) { return values!=null && values.size()==1 ? values.getFirst() : null; }
}
