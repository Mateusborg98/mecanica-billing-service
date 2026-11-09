package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.time.Clock;
import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import br.com.techchallenge.mecanica.billing.application.usecase.ConfirmarPagamentoUseCase;
public class PagamentoInboxWorker {
    private final MongoTemplate mongo; private final ConfirmarPagamentoUseCase useCase; private final Clock clock;
    public PagamentoInboxWorker(MongoTemplate mongo,ConfirmarPagamentoUseCase useCase,Clock clock) {
        this.mongo=mongo; this.useCase=useCase; this.clock=clock;
    }
    @Scheduled(fixedDelayString="${mercado-pago.webhook.delay-ms:2000}")
    public boolean processarProxima() {
        var now=clock.instant(); var token=UUID.randomUUID().toString();
        var filter=Criteria.where("pendente").is(true).and("proximaTentativa").lte(Date.from(now))
            .orOperator(Criteria.where("leaseAte").exists(false),Criteria.where("leaseAte").lte(Date.from(now)));
        var doc=mongo.findAndModify(Query.query(filter),new Update().set("leaseToken",token)
            .set("leaseAte",Date.from(now.plusSeconds(30))).inc("tentativas",1),
            FindAndModifyOptions.options().returnNew(true),Document.class,"notificacoes_pagamento");
        if(doc==null) return false;
        var dono=Criteria.where("_id").is(doc.getString("_id")).and("leaseToken").is(token);
        try { useCase.executar(doc.getString("_id")); }
        catch(RuntimeException e) {
            mongo.updateFirst(Query.query(dono),new Update().set("ultimoErro",e.getClass().getSimpleName())
                .set("proximaTentativa",Date.from(clock.instant().plusSeconds(30)))
                .unset("leaseAte").unset("leaseToken"),"notificacoes_pagamento");
            return false;
        }
        // A notification arriving during GET must remain pending for another authoritative lookup.
        mongo.updateFirst(Query.query(new Criteria().andOperator(dono,Criteria.where("versao").is(doc.get("versao")))),
            new Update().set("pendente",false).set("processadaEm",Date.from(clock.instant())).unset("ultimoErro"),"notificacoes_pagamento");
        mongo.updateFirst(Query.query(dono),new Update().unset("leaseAte").unset("leaseToken"),"notificacoes_pagamento");
        return true;
    }
}
