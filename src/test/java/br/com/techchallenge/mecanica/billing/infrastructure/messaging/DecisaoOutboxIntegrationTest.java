package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import com.mongodb.client.*;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS", matches="true")
class DecisaoOutboxIntegrationTest {
    private MongoClient client;
    private MongoTemplate mongo;
    private OrcamentoGatewayImpl gateway;
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
    @BeforeEach void setup() {
        client = MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI",
                "mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin&serverSelectionTimeoutMS=5000"));
        mongo = new MongoTemplate(client, "billing_decisao_test_" + UUID.randomUUID().toString().replace("-", ""));
        gateway = new OrcamentoGatewayImpl(new MongoRepositoryFactory(mongo).getRepository(OrcamentoMongoRepository.class),
                new OrcamentoPersistenceMapper(), mongo);
    }
    @AfterEach void cleanup() { if (mongo != null) mongo.getDb().drop(); if (client != null) client.close(); }
    private Orcamento criar() {
        return new ReceberDiagnosticoUseCase(new OrcamentoDiagnosticoMongoGateway(mongo, new OrcamentoPersistenceMapper()))
                .executar(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), List.of(new ItemOrcamento(
                        UUID.randomUUID(), TipoItemOrcamento.SERVICO, "Revisão", 1, BigDecimal.TEN)));
    }
    private Document doc(Orcamento o) { return mongo.findById(o.getId().toString(), Document.class, "orcamentos"); }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void decisaoEEventoDevemSerAtomicos(boolean aprovar) {
        var o = criar(); var concorrente = gateway.buscarPorId(o.getId()).orElseThrow();
        if (aprovar) { o.aprovar(); concorrente.recusar(); } else { o.recusar(); concorrente.aprovar(); }
        gateway.salvar(o); var antes = doc(o).get("decisao", Document.class);
        assertThrows(br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException.class, () -> gateway.salvar(concorrente));
        assertEquals(antes, doc(o).get("decisao", Document.class));
        var enviados = new ArrayList<OrcamentoDecidido>();
        var worker = new DecisaoOutboxWorker(mongo, enviados::add, clock);
        assertTrue(worker.publicarProximo()); assertFalse(worker.publicarProximo());
        assertEquals(o.getStatus().name(), enviados.getFirst().decisao());
        assertEquals(o.getId(), enviados.getFirst().eventId());
        assertEquals(o.getOrdemId(), enviados.getFirst().ordemId());
        assertFalse(doc(o).get("decisao", Document.class).getBoolean("pendente"));
    }
    @Test void falhaDePublicacaoDeveManterDecisaoEPermitirRetentativa() {
        var o=criar(); o.aprovar(); gateway.salvar(o);
        var worker = new DecisaoOutboxWorker(mongo, event -> { throw new IllegalStateException("broker indisponível"); }, clock);
        assertFalse(worker.publicarProximo()); assertFalse(worker.publicarProximo());
        assertEquals(StatusOrcamento.APROVADO, gateway.buscarPorId(o.getId()).orElseThrow().getStatus());
        var eventos = new ArrayList<OrcamentoDecidido>();
        assertTrue(new DecisaoOutboxWorker(mongo, eventos::add, Clock.offset(clock, Duration.ofSeconds(31))).publicarProximo());
        assertEquals(2, doc(o).get("decisao", Document.class).getInteger("tentativas"));
    }
    @Test void leaseDePublicadorInterrompidoDeveExpirar() {
        var o=criar(); o.recusar(); gateway.salvar(o);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(o.getId().toString())), new Update()
                .set("decisao.leaseToken", "processo-encerrado").set("decisao.leaseAte", Date.from(clock.instant().plusSeconds(30))), "orcamentos");
        var eventos = new ArrayList<OrcamentoDecidido>();
        assertFalse(new DecisaoOutboxWorker(mongo, eventos::add, clock).publicarProximo());
        assertTrue(new DecisaoOutboxWorker(mongo, eventos::add, Clock.offset(clock, Duration.ofSeconds(31))).publicarProximo());
    }
    @Test void doisPublicadoresNaoDevemAdquirirMesmoDocumento() throws Exception {
        var o=criar(); o.aprovar(); gateway.salvar(o);
        var entrou=new java.util.concurrent.CountDownLatch(1); var liberar=new java.util.concurrent.CountDownLatch(1);
        Consumer<OrcamentoDecidido> lento = event -> {
            entrou.countDown();
            try { if (!liberar.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout"); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
        };
        try (var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var primeiro=pool.submit(() -> new DecisaoOutboxWorker(mongo,lento,clock).publicarProximo());
            try {
                assertTrue(entrou.await(5,java.util.concurrent.TimeUnit.SECONDS));
                assertFalse(new DecisaoOutboxWorker(mongo,event -> fail("Publicou duas vezes"),clock).publicarProximo());
            } finally { liberar.countDown(); }
            assertTrue(primeiro.get(5,java.util.concurrent.TimeUnit.SECONDS));
        }
    }
    @Test void orcamentoManualNaoDeveGerarMensagemParaExecution() {
        var o=new Orcamento(UUID.randomUUID(),List.of(new ItemOrcamento(UUID.randomUUID(),TipoItemOrcamento.SERVICO,"Manual",1,BigDecimal.TEN)),Instant.now());
        gateway.salvar(o); o.aprovar(); gateway.salvar(o);
        assertFalse(new DecisaoOutboxWorker(mongo,event -> fail("Manual sem reparo"),clock).publicarProximo());
    }
}
