package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
public class DecisaoOutboxWorker {
    private final MongoTemplate mongo;
    private final Consumer<OrcamentoDecidido> publisher;
    private final Clock clock;
    private final Consumer<DecisaoOrcamentoComValor> publisherV2;
    public DecisaoOutboxWorker(MongoTemplate mongo, Consumer<OrcamentoDecidido> publisher, Clock clock, Consumer<DecisaoOrcamentoComValor> publisherV2) {
        this.mongo = mongo; this.publisher = publisher; this.clock = clock; this.publisherV2=publisherV2;
    }
    @Scheduled(fixedDelayString="${integration.decisao-outbox.delay-ms:2000}")
    public boolean publicarProximo() {
        var agora = Date.from(clock.instant()); var token = UUID.randomUUID().toString();
        var filtro = Criteria.where("decisao.pendente").is(true).and("reparoId").ne(null)
                .and("decisao.proximaTentativa").lte(agora)
                .orOperator(Criteria.where("decisao.leaseAte").exists(false), Criteria.where("decisao.leaseAte").lte(agora));
        var doc = mongo.findAndModify(Query.query(filtro), new Update()
                .set("decisao.leaseToken", token).set("decisao.leaseAte", Date.from(clock.instant().plusSeconds(30)))
                .inc("decisao.tentativas", 1), FindAndModifyOptions.options().returnNew(true), Document.class, "orcamentos");
        if (doc == null) return false;
        var dono = Query.query(Criteria.where("_id").is(doc.getString("_id")).and("decisao.leaseToken").is(token));
        try {
            var decisao = doc.get("decisao", Document.class);
            var evento=new OrcamentoDecidido(1, "OrcamentoDecidido", UUID.fromString(decisao.getString("eventId")),
                    UUID.fromString(doc.getString("_id")), UUID.fromString(doc.getString("reparoId")),
                    UUID.fromString(doc.getString("ordemId")), doc.getString("status"), decisao.getDate("ocorridoEm").toInstant());
            var total=doc.getList("itens",Document.class).stream().map(item ->
                new java.math.BigDecimal(item.get("valorUnitario").toString()).multiply(java.math.BigDecimal.valueOf(item.getInteger("quantidade"))))
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
            publisher.accept(evento);
            publisherV2.accept(new DecisaoOrcamentoComValor(2,"OrcamentoDecidido",evento.eventId(),evento.orcamentoId(),
                evento.reparoId(),evento.ordemId(),evento.decisao(),evento.ocorridoEm(),total,"BRL"));
        } catch (RuntimeException exception) {
            mongo.updateFirst(dono, new Update().set("decisao.proximaTentativa", Date.from(clock.instant().plusSeconds(30)))
                    .set("decisao.ultimoErro", exception.getClass().getSimpleName())
                    .unset("decisao.leaseAte").unset("decisao.leaseToken"), "orcamentos");
            return false;
        }
        mongo.updateFirst(dono, new Update().set("decisao.pendente", false).set("decisao.publicadoEm", Date.from(clock.instant()))
                .unset("decisao.ultimoErro").unset("decisao.leaseAte").unset("decisao.leaseToken"), "orcamentos");
        return true;
    }
}
