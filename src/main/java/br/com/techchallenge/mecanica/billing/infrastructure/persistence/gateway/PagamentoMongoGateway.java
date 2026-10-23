package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.OrderPagamento;
import br.com.techchallenge.mecanica.billing.application.gateway.PagamentoGateway;
import br.com.techchallenge.mecanica.billing.application.exception.PagamentoDivergenteException;
public class PagamentoMongoGateway implements PagamentoGateway {
    private final MongoTemplate mongo;
    public PagamentoMongoGateway(MongoTemplate mongo) { this.mongo=mongo; }
    public void confirmar(OrderPagamento order,Instant confirmadoEm) {
        if(!order.creditada() || order.total().signum()<=0 || order.total().compareTo(order.pago())!=0)
            throw new PagamentoDivergenteException();
        var base=Criteria.where("_id").is(order.cobrancaId().toString()).and("status").is("APROVADO")
            .and("cobranca.moeda").is(order.moeda()).and("cobranca.valorTotal").is(new Decimal128(order.total()));
        var query=Query.query(new Criteria().andOperator(base,Criteria.where("cobranca.status").is("PENDENTE")));
        var comprovante=new Document("orderId",order.orderId()).append("valorPago",new Decimal128(order.pago()))
            .append("confirmadoEm",Date.from(confirmadoEm)).append("atualizadaNoProvedorEm",Date.from(order.atualizadaEm()));
        var updated=mongo.updateFirst(query,new Update().set("cobranca.status","PAGA").set("pagamento",comprovante),"orcamentos");
        if(updated.getModifiedCount()==1) return;
        var repetida=Query.query(new Criteria().andOperator(base,Criteria.where("cobranca.status").is("PAGA")
            .and("pagamento.orderId").is(order.orderId()).and("pagamento.valorPago").is(new Decimal128(order.pago()))));
        if(!mongo.exists(repetida,"orcamentos")) throw new PagamentoDivergenteException();
    }
}
