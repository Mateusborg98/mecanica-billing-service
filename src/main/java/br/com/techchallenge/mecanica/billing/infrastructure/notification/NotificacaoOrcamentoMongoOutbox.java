package br.com.techchallenge.mecanica.billing.infrastructure.notification;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.dao.DuplicateKeyException;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoOrcamentoGateway;
import br.com.techchallenge.mecanica.billing.application.exception.*;
public final class NotificacaoOrcamentoMongoOutbox implements NotificacaoOrcamentoGateway {
    private static final String COLLECTION="envios_orcamento";
    public record Pendente(UUID id,String destinatario,String texto,String token) {}
    private final MongoTemplate mongo;
    public NotificacaoOrcamentoMongoOutbox(MongoTemplate mongo) {this.mongo=mongo;}
    public void agendar(UUID id,String destinatario,String texto,Instant agora) {
        var doc=new Document("_id",id.toString()).append("destinatario",destinatario).append("texto",texto)
            .append("status","PENDENTE").append("tentativas",0).append("criadoEm",Date.from(agora)).append("proximaTentativa",Date.from(agora));
        try {mongo.insert(doc,COLLECTION);}
        catch(DuplicateKeyException ex) {
            var anterior=mongo.findById(id.toString(),Document.class,COLLECTION);
            if(anterior==null || !destinatario.equals(anterior.getString("destinatario")) || !texto.equals(anterior.getString("texto")))
                throw new ConflitoOrcamentoException(id);
        }
    }
    public Situacao buscar(UUID id) {
        var doc=mongo.findById(id.toString(),Document.class,COLLECTION);
        if(doc==null) throw new OrcamentoNaoEncontradoException(id);
        return new Situacao(id,doc.getString("status"),doc.getInteger("tentativas"));
    }
    public Pendente reservar(Instant agora) {
        var criteria=new Criteria().andOperator(Criteria.where("status").is("PENDENTE"),
            Criteria.where("proximaTentativa").lte(Date.from(agora)),new Criteria().orOperator(
                Criteria.where("leaseAte").exists(false),Criteria.where("leaseAte").lte(Date.from(agora))));
        String token=UUID.randomUUID().toString();
        var doc=mongo.findAndModify(Query.query(criteria).with(Sort.by("criadoEm","_id")),
            new Update().set("leaseToken",token).set("leaseAte",Date.from(agora.plusSeconds(60))).inc("tentativas",1),
            FindAndModifyOptions.options().returnNew(true),Document.class,COLLECTION);
        return doc==null?null:new Pendente(UUID.fromString(doc.getString("_id")),doc.getString("destinatario"),doc.getString("texto"),token);
    }
    public void concluir(Pendente p,String status) {
        mongo.updateFirst(reserva(p),new Update().set("status",status).unset("leaseToken").unset("leaseAte").unset("ultimoErro"),COLLECTION);
    }
    public void reagendar(Pendente p,Instant agora,RuntimeException erro) {
        mongo.updateFirst(reserva(p),new Update().set("proximaTentativa",Date.from(agora.plusSeconds(30)))
            .set("ultimoErro",erro.getClass().getSimpleName()).unset("leaseToken").unset("leaseAte"),COLLECTION);
    }
    private Query reserva(Pendente p) {return Query.query(Criteria.where("_id").is(p.id().toString()).and("leaseToken").is(p.token()));}
}
