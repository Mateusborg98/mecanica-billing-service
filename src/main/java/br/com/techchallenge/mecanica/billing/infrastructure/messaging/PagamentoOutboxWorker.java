package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
public class PagamentoOutboxWorker {
    private final MongoTemplate mongo;
    private final Consumer<PagamentoConfirmado> publisher;
    private final Clock clock;
    public PagamentoOutboxWorker(MongoTemplate mongo, Consumer<PagamentoConfirmado> publisher, Clock clock) {
        this.mongo = mongo; this.publisher = publisher; this.clock = clock;
    }
    @Scheduled(fixedDelayString="${integration.pagamento-outbox.delay-ms:2000}")
    public boolean publicarProximo() {
        var agora = Date.from(clock.instant()); var token = UUID.randomUUID().toString();
        var filtro = Criteria.where("pagamentoEvento.pendente").is(true).and("reparoId").ne(null)
                .and("pagamentoEvento.proximaTentativa").lte(agora)
                .orOperator(Criteria.where("pagamentoEvento.leaseAte").exists(false), Criteria.where("pagamentoEvento.leaseAte").lte(agora));
        var doc = mongo.findAndModify(Query.query(filtro), new Update()
                .set("pagamentoEvento.leaseToken", token).set("pagamentoEvento.leaseAte", Date.from(clock.instant().plusSeconds(30)))
                .inc("pagamentoEvento.tentativas", 1), FindAndModifyOptions.options().returnNew(true), Document.class, "orcamentos");
        if (doc == null) return false;
        var dono = Query.query(Criteria.where("_id").is(doc.getString("_id")).and("pagamentoEvento.leaseToken").is(token));
        try {
            var evento = doc.get("pagamentoEvento", Document.class);
            var cobranca = doc.get("cobranca", Document.class);
            publisher.accept(new PagamentoConfirmado(1, "PagamentoConfirmado", UUID.fromString(evento.getString("eventId")),
                    UUID.fromString(doc.getString("_id")), UUID.fromString(doc.getString("ordemId")),
                    UUID.fromString(doc.getString("reparoId")), cobranca.get("valorTotal", org.bson.types.Decimal128.class).bigDecimalValue(),
                    cobranca.getString("moeda"), evento.getDate("confirmadoEm").toInstant()));
        } catch (RuntimeException exception) {
            mongo.updateFirst(dono, new Update().set("pagamentoEvento.proximaTentativa", Date.from(clock.instant().plusSeconds(30)))
                    .set("pagamentoEvento.ultimoErro", exception.getClass().getSimpleName())
                    .unset("pagamentoEvento.leaseAte").unset("pagamentoEvento.leaseToken"), "orcamentos");
            return false;
        }
        mongo.updateFirst(dono, new Update().set("pagamentoEvento.pendente", false).set("pagamentoEvento.publicadoEm", Date.from(clock.instant()))
                .unset("pagamentoEvento.ultimoErro").unset("pagamentoEvento.leaseAte").unset("pagamentoEvento.leaseToken"), "orcamentos");
        return true;
    }
}
