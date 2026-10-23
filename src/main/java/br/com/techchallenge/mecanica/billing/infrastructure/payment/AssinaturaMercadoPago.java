package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException;
public class AssinaturaMercadoPago {
    private final String secret;
    public AssinaturaMercadoPago(String secret) { this.secret=secret; }
    public void validar(String orderId,String requestId,String signature) {
        if(secret==null || secret.isBlank()) throw new CheckoutIndisponivelException();
        if(orderId==null || !orderId.matches("[A-Za-z0-9]{1,80}") || requestId==null
                || !requestId.matches("[A-Za-z0-9-]{1,100}") || signature==null || signature.length()>1024)
            throw new AssinaturaWebhookInvalidaException();
        try {
            var fields=new HashMap<String,String>();
            for(var part:signature.split(",")) {
                var pair=part.trim().split("=",2);
                if(pair.length!=2 || fields.putIfAbsent(pair[0].trim(),pair[1].trim())!=null)
                    throw new AssinaturaWebhookInvalidaException();
            }
            var ts=fields.getOrDefault("ts",""); var digest=fields.getOrDefault("v1","");
            if(!ts.matches("[0-9]{1,16}") || !digest.matches("[a-fA-F0-9]{64}")) throw new AssinaturaWebhookInvalidaException();
            // Orders SDK contract: preserve data.id case; sign the query ID, never body status/value.
            var manifest="id:"+orderId+";request-id:"+requestId+";ts:"+ts+";";
            var mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            if(!MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(digest)))
                throw new AssinaturaWebhookInvalidaException();
        } catch(AssinaturaWebhookInvalidaException e) { throw e; }
        catch(Exception e) { throw new AssinaturaWebhookInvalidaException(); }
    }
}
