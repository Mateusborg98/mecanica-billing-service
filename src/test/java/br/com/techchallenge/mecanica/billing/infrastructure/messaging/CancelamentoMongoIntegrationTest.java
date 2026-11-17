package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.mongodb.client.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import br.com.techchallenge.mecanica.billing.application.dto.CancelamentoReparoInput;
import br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS",matches="true")
class CancelamentoMongoIntegrationTest {
    MongoClient client;MongoTemplate mongo;OrcamentoGatewayImpl orcamentos;CancelamentoReparoMongoGateway cancelamentos;
    final UUID reparo=UUID.randomUUID(),ordem=UUID.randomUUID(),item=UUID.randomUUID();
    final Instant data=Instant.parse("2026-11-03T12:00:00Z");
    CancelamentoReparoInput evento() { return new CancelamentoReparoInput(1,"ReparoCancelado",reparo,reparo,ordem,data.plusSeconds(10)); }
    @BeforeEach void setup() {
        client=MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI","mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin"));
        mongo=new MongoTemplate(client,"billing_cancelamento_test_"+UUID.randomUUID().toString().replace("-",""));
        cancelamentos=new CancelamentoReparoMongoGateway(mongo);
        orcamentos=new OrcamentoGatewayImpl(new MongoRepositoryFactory(mongo).getRepository(OrcamentoMongoRepository.class),new OrcamentoPersistenceMapper(),mongo);
    }
    @AfterEach void cleanup() { if(mongo!=null)mongo.getDb().drop();if(client!=null)client.close(); }
    Orcamento diagnostico() { return new ReceberDiagnosticoUseCase(new OrcamentoDiagnosticoMongoGateway(mongo,new OrcamentoPersistenceMapper()))
        .executar(reparo,ordem,data,List.of(new ItemOrcamento(item,TipoItemOrcamento.SERVICO,"Inspeção",1,BigDecimal.TEN))); }
    @Test void cancelaAprovadoSemReabrirOuPublicarDecisaoObsoleta() {
        var o=diagnostico();o.aprovar();orcamentos.salvar(o);cancelamentos.registrar(evento());cancelamentos.registrar(evento());
        assertEquals(StatusOrcamento.CANCELADO,orcamentos.buscarPorId(o.getId()).orElseThrow().getStatus());
        var d=mongo.findById(o.getId().toString(),Document.class,"orcamentos");assertFalse(d.containsKey("decisao"));assertNull(d.get("cobranca"));
        assertThrows(br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException.class,()->orcamentos.buscarPorId(o.getId()).orElseThrow().aprovar());
    }
    @Test void cancelamentoAntesDoDiagnosticoPreservaSnapshotSemCriarOrcamentoVazio() {
        cancelamentos.registrar(evento());assertTrue(orcamentos.buscarPorId(evento().orcamentoId()).isEmpty());
        assertEquals(StatusOrcamento.CANCELADO,diagnostico().getStatus());assertEquals(StatusOrcamento.CANCELADO,diagnostico().getStatus());
        assertEquals(BigDecimal.TEN,orcamentos.buscarPorId(evento().orcamentoId()).orElseThrow().calcularTotal());
    }
    @Test void decisaoLidaAntesDoCancelamentoNaoPodeSerSalvaDepois() {
        var o=diagnostico();o.aprovar();cancelamentos.registrar(evento());
        assertThrows(br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException.class,()->orcamentos.salvar(o));
    }
    @Test void eventoConflitanteECobrancaExistenteSaoPreservados() {
        var o=diagnostico();cancelamentos.registrar(evento());
        assertThrows(IllegalArgumentException.class,()->cancelamentos.registrar(new CancelamentoReparoInput(1,"ReparoCancelado",reparo,reparo,UUID.randomUUID(),data)));
        assertThrows(IllegalArgumentException.class,()->cancelamentos.registrar(new CancelamentoReparoInput(1,"ReparoCancelado",reparo,reparo,ordem,data)));
        mongo.updateFirst(Query.query(Criteria.where("_id").is(o.getId().toString())),new Update().set("cobranca",new Document("status","PAGA")),"orcamentos");
        assertThrows(IllegalArgumentException.class,()->cancelamentos.registrar(evento()));
        assertEquals("PAGA",mongo.findById(o.getId().toString(),Document.class,"orcamentos").get("cobranca",Document.class).getString("status"));
    }
    @Test void cancelamentoEDiagnosticoConcorrentesConvergem() throws Exception {
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{start.await();diagnostico();return true;});
            var b=pool.submit(()->{start.await();cancelamentos.registrar(evento());return true;});start.countDown();
            assertTrue(a.get(15,java.util.concurrent.TimeUnit.SECONDS));assertTrue(b.get(15,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(StatusOrcamento.CANCELADO,orcamentos.buscarPorId(evento().orcamentoId()).orElseThrow().getStatus());
        assertEquals(1,mongo.getCollection("orcamentos").countDocuments());
    }
}
