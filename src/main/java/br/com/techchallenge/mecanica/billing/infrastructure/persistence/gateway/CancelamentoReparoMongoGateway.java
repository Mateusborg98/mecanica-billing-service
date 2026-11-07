package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.CancelamentoReparoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.CancelamentoReparoGateway;
public class CancelamentoReparoMongoGateway implements CancelamentoReparoGateway {
    private final MongoTemplate mongo;
    public CancelamentoReparoMongoGateway(MongoTemplate mongo) { this.mongo=mongo; }
    public void registrar(CancelamentoReparoInput e) {
        var criteria=new Criteria().andOperator(
            Criteria.where("_id").is(e.orcamentoId().toString()),Criteria.where("cobranca").is(null),
            new Criteria().orOperator(Criteria.where("ordemId").is(e.ordemId().toString()),Criteria.where("ordemId").exists(false)),
            new Criteria().orOperator(Criteria.where("reparoId").is(e.reparoId().toString()),Criteria.where("reparoId").exists(false)),
            new Criteria().orOperator(Criteria.where("cancelamento.conteudo").is(e.toString()),Criteria.where("cancelamento").exists(false)));
        // One atomic document: tombstone, status and inbox. A late diagnosis only fills the snapshot.
        var update=new Update().set("ordemId",e.ordemId().toString()).set("reparoId",e.reparoId().toString())
            .set("status","CANCELADO").set("cancelamento.conteudo",e.toString())
            .set("cancelamento.ocorridoEm",java.util.Date.from(e.ocorridoEm())).unset("decisao");
        try { mongo.upsert(Query.query(criteria),update,"orcamentos"); }
        catch(DuplicateKeyException exception) {
            // A diagnosis or equivalent cancellation may have won the concurrent insert.
            if(mongo.updateFirst(Query.query(criteria),update,"orcamentos").getMatchedCount()==1) return;
            throw new IllegalArgumentException("Cancelamento conflitante ou orçamento com cobrança",exception);
        }
    }
}
