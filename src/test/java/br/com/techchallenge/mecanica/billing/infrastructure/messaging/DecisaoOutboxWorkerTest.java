package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
class DecisaoOutboxWorkerTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"),ZoneOffset.UTC);
    private Document pendente() {
        var id=UUID.randomUUID().toString();
        return new Document("_id",id).append("reparoId",UUID.randomUUID().toString())
                .append("ordemId",UUID.randomUUID().toString()).append("status","APROVADO").append("itens",List.of(new Document("quantidade",2).append("valorUnitario",new org.bson.types.Decimal128(new java.math.BigDecimal("25.50")))))
                .append("decisao",new Document("eventId",id).append("ocorridoEm",Date.from(clock.instant())));
    }
    @Test void falhaNoMongoDepoisDoAckDevePermitirReentregaComMesmoIdEConteudo() {
        var mongo=mock(MongoTemplate.class); var doc=pendente();
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(Document.class),eq("orcamentos")))
                .thenReturn(doc,doc,null);
        // The broker accepted the event, but the process cannot persist the acknowledgement.
        when(mongo.updateFirst(any(Query.class),any(Update.class),eq("orcamentos")))
                .thenThrow(new DataAccessResourceFailureException("Conexao encerrada depois do ACK"))
                .thenReturn(com.mongodb.client.result.UpdateResult.acknowledged(1,1L,null));
        var enviados=new ArrayList<OrcamentoDecidido>();
        var worker=new DecisaoOutboxWorker(mongo,enviados::add,clock, v2 -> {});
        assertThrows(DataAccessResourceFailureException.class,worker::publicarProximo);
        assertTrue(new DecisaoOutboxWorker(mongo,enviados::add,Clock.offset(clock,Duration.ofSeconds(31)), v2 -> {}).publicarProximo());
        assertEquals(2,enviados.size()); assertEquals(enviados.getFirst(),enviados.getLast());
        assertFalse(worker.publicarProximo());
    }
    @Test void falhaNoBrokerNaoDeveMarcarEventoComoPublicado() {
        var mongo=mock(MongoTemplate.class);
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(Document.class),eq("orcamentos")))
                .thenReturn(pendente());
        var worker=new DecisaoOutboxWorker(mongo,event->{throw new IllegalStateException("Sem confirmacao");},clock, v2 -> {});
        assertFalse(worker.publicarProximo());
        var update=org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class),update.capture(),eq("orcamentos"));
        var campos=update.getValue().getUpdateObject().get("$set",Document.class);
        assertFalse(campos.containsKey("decisao.publicadoEm"));
        assertFalse(campos.containsKey("decisao.pendente"));
        assertEquals(Date.from(clock.instant().plusSeconds(30)),campos.get("decisao.proximaTentativa"));
    }
    @Test void falhaDaPublicacaoV2MantemOutboxPendenteMesmoComV1Confirmado() {
        var mongo=mock(MongoTemplate.class);
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(Document.class),eq("orcamentos"))).thenReturn(pendente());
        var antigos=new ArrayList<OrcamentoDecidido>();
        var worker=new DecisaoOutboxWorker(mongo,antigos::add,clock,e->{
            assertEquals(0,new java.math.BigDecimal("51").compareTo(e.valorTotal()));
            throw new IllegalStateException("V2 indisponível");
        });
        assertFalse(worker.publicarProximo());assertEquals(1,antigos.size());
        var update=org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class),update.capture(),eq("orcamentos"));
        assertFalse(update.getValue().getUpdateObject().get("$set",Document.class).containsKey("decisao.pendente"));
    }
}
