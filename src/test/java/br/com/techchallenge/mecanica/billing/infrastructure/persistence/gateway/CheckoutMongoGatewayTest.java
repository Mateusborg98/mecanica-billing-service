package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
class CheckoutMongoGatewayTest {
    @Test void retornaPersistidoSemSobrescrever() {
        var mongo=mock(MongoTemplate.class); var gateway=new CheckoutMongoGateway(mongo); var id=UUID.randomUUID();
        assertTrue(gateway.buscar(id).isEmpty());
        var record=new CheckoutMongoGateway.Registro(id.toString(),"ORD1","https://www.mercadopago.com.br/checkout");
        when(mongo.findById(id.toString(),CheckoutMongoGateway.Registro.class)).thenReturn(record);
        assertEquals("ORD1",gateway.buscar(id).orElseThrow().orderId());
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(CheckoutMongoGateway.Registro.class))).thenReturn(record);
        assertEquals("ORD1",gateway.salvarSeAusente(id,new Checkout("ORD2","ignored")).orderId());
    }
}
