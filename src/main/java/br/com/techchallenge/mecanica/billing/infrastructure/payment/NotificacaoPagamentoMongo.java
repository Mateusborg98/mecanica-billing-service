package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import java.time.Clock;
import java.util.Date;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoPagamentoGateway;
public class NotificacaoPagamentoMongo implements NotificacaoPagamentoGateway {
    private final MongoTemplate mongo;
    private final Clock clock;
    public NotificacaoPagamentoMongo(MongoTemplate mongo,Clock clock) { this.mongo=mongo; this.clock=clock; }
    public void registrar(String orderId) {
        mongo.upsert(Query.query(Criteria.where("_id").is(orderId)),new Update().set("pendente",true)
            .set("proximaTentativa",Date.from(clock.instant())).inc("versao",1).setOnInsert("tentativas",0),"notificacoes_pagamento");
    }
}
