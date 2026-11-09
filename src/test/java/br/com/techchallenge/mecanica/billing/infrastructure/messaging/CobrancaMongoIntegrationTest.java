package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import com.mongodb.client.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS",matches="true")
class CobrancaMongoIntegrationTest {
    private MongoClient client;
    private MongoTemplate mongo;
    private OrcamentoGatewayImpl orcamentos;
    private CobrancaMongoGateway cobrancas;
    private ReceberDiagnosticoUseCase diagnosticos;
    private final UUID reparoId=UUID.randomUUID(), ordemId=UUID.randomUUID();
    private final Instant criado=Instant.parse("2026-10-20T12:00:00Z");
    private final Instant concluido=Instant.parse("2026-10-20T15:00:00.123456Z");
    private final List<ItemOrcamento> itens=List.of(new ItemOrcamento(UUID.randomUUID(),TipoItemOrcamento.PECA,"Filtro",2,new BigDecimal("25.50")));
    @BeforeEach void setup() {
        client=MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI","mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin&serverSelectionTimeoutMS=5000"));
        mongo=new MongoTemplate(client,"billing_cobranca_test_"+UUID.randomUUID().toString().replace("-",""));
        var mapper=new OrcamentoPersistenceMapper();
        orcamentos=new OrcamentoGatewayImpl(new MongoRepositoryFactory(mongo).getRepository(OrcamentoMongoRepository.class),mapper,mongo);
        cobrancas=new CobrancaMongoGateway(mongo,mapper);
        diagnosticos=new ReceberDiagnosticoUseCase(new OrcamentoDiagnosticoMongoGateway(mongo,mapper));
    }
    @AfterEach void cleanup() { if(mongo!=null) mongo.getDb().drop(); if(client!=null) client.close(); }
    private Orcamento criar() { return diagnosticos.executar(reparoId,ordemId,criado,itens); }
    private ConclusaoReparoInput input(UUID id) { return new ConclusaoReparoInput(reparoId,reparoId,ordemId,id,concluido); }
    private Cobranca concluir(UUID id) { return cobrancas.criarOuRecuperar(input(id),concluido.plusSeconds(1)); }
    @Test void deveCriarCobrancaPendenteComValorDoOrcamentoSemDuplicar() {
        var o=criar(); o.aprovar(); orcamentos.salvar(o);
        assertTrue(cobrancas.buscar(o.getId()).isEmpty());
        var c=concluir(o.getId()); var repetida=cobrancas.criarOuRecuperar(input(o.getId()),concluido.plusSeconds(100));
        assertEquals(c,repetida); assertEquals(c,cobrancas.buscar(o.getId()).orElseThrow());
        assertEquals(Cobranca.Status.PENDENTE,c.status()); assertEquals("BRL",c.moeda());
        assertEquals(0,new BigDecimal("51.00").compareTo(c.valorTotal()));
        assertEquals(concluido.truncatedTo(java.time.temporal.ChronoUnit.MILLIS),c.reparoConcluidoEm());
        diagnosticos.executar(reparoId,ordemId,criado,itens);
        assertEquals(c,cobrancas.buscar(o.getId()).orElseThrow());
    }
    @Test void naoDeveCobrarOrcamentoPendenteOuRecusado() {
        var o=criar(); assertThrows(IllegalArgumentException.class,()->concluir(o.getId()));
        o.recusar(); orcamentos.salvar(o); assertThrows(IllegalArgumentException.class,()->concluir(o.getId()));
        assertTrue(cobrancas.buscar(o.getId()).isEmpty());
    }
    @Test void naoDeveAceitarConclusaoDeOutraOrdemOuDataAnterior() {
        var o=criar(); o.aprovar(); orcamentos.salvar(o);
        assertThrows(IllegalArgumentException.class,()->cobrancas.criarOuRecuperar(new ConclusaoReparoInput(
                reparoId,reparoId,UUID.randomUUID(),o.getId(),concluido),concluido));
        assertThrows(IllegalArgumentException.class,()->cobrancas.criarOuRecuperar(new ConclusaoReparoInput(
                reparoId,reparoId,ordemId,o.getId(),criado.minusSeconds(1)),concluido));
        assertTrue(cobrancas.buscar(o.getId()).isEmpty());
    }
    @Test void reentregaComDataDiferenteNaoDeveSobrescreverCobranca() {
        var o=criar(); o.aprovar(); orcamentos.salvar(o); var c=concluir(o.getId());
        assertThrows(IllegalArgumentException.class,()->cobrancas.criarOuRecuperar(new ConclusaoReparoInput(
                reparoId,reparoId,ordemId,o.getId(),concluido.plusSeconds(1)),concluido));
        assertEquals(c,cobrancas.buscar(o.getId()).orElseThrow());
    }
    @Test void conclusoesConcorrentesDevemRecuperarMesmaCobranca() throws Exception {
        var o=criar(); o.aprovar(); orcamentos.salvar(o); var largada=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{largada.await(); return concluir(o.getId());});
            var b=pool.submit(()->{largada.await(); return cobrancas.criarOuRecuperar(input(o.getId()),concluido.plusSeconds(9));});
            largada.countDown(); assertEquals(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1,mongo.getCollection("orcamentos").countDocuments(new org.bson.Document("cobranca.eventoId",reparoId.toString())));
    }
}
