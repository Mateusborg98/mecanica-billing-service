package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import com.mongodb.client.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS",matches="true")
class CheckoutMongoIntegrationTest {
    @Test void concorrenciaPersisteUmCheckoutEReinicioRecupera() throws Exception {
        try(var client=MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI","mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin"))) {
            var mongo=new MongoTemplate(client,"checkout_test_"+UUID.randomUUID().toString().replace("-",""));
            try {
                var gateway=new CheckoutMongoGateway(mongo); var id=UUID.randomUUID();
                mongo.createCollection(CheckoutMongoGateway.Registro.class);
                var checkout=new Checkout("ORD1","https://www.mercadopago.com.br/checkout");
                try(var pool=Executors.newFixedThreadPool(4)) {
                    var tasks=new ArrayList<Callable<Checkout>>();
                    for(int i=0;i<8;i++) tasks.add(()->gateway.salvarSeAusente(id,checkout));
                    for(var f:pool.invokeAll(tasks)) assertEquals(checkout,f.get());
                }
                assertEquals(1,mongo.getCollection("checkouts").countDocuments());
                assertEquals(checkout,new CheckoutMongoGateway(mongo).buscar(id).orElseThrow());
                assertEquals(checkout,gateway.salvarSeAusente(id,new Checkout("ORD2","ignored")));
            } finally { mongo.getDb().drop(); }
        }
    }
}
