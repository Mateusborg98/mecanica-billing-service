package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import java.util.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.gateway.CheckoutGateway;
public class CheckoutMongoGateway implements CheckoutGateway {
    @Document("checkouts")
    public record Registro(@Id String id, String orderId, String checkoutUrl) {
        Checkout checkout() { return new Checkout(orderId,checkoutUrl); }
    }
    private final MongoTemplate mongo;
    public CheckoutMongoGateway(MongoTemplate mongo) { this.mongo=mongo; }
    public Optional<Checkout> buscar(UUID id) {
        return Optional.ofNullable(mongo.findById(id.toString(),Registro.class)).map(Registro::checkout);
    }
    public Checkout salvarSeAusente(UUID id, Checkout checkout) {
        var result=mongo.findAndModify(Query.query(Criteria.where("_id").is(id.toString())),
            new Update().setOnInsert("orderId",checkout.orderId()).setOnInsert("checkoutUrl",checkout.checkoutUrl()),
            FindAndModifyOptions.options().upsert(true).returnNew(true),Registro.class);
        return Objects.requireNonNull(result).checkout();
    }
}
