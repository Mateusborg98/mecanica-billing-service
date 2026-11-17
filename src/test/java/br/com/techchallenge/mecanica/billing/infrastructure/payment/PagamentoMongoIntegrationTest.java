package br.com.techchallenge.mecanica.billing.infrastructure.payment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.mongodb.client.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.*;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.application.exception.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS",matches="true")
class PagamentoMongoIntegrationTest {
    MongoClient client; MongoTemplate mongo; CobrancaService charges; CheckoutMongoGateway checkouts;
    PagamentoMongoGateway payments; NotificacaoPagamentoMongo inbox; ConfirmarPagamentoUseCase usecase;
    final ConsultaPagamento provider=mock(ConsultaPagamento.class);
    final UUID id=UUID.randomUUID(); final Instant now=Instant.parse("2026-10-23T12:00:00Z");
    final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    OrderPagamento valid() { return new OrderPagamento("ORDTEST",id,"processed","accredited","BRL",new BigDecimal("51.00"),new BigDecimal("51.00"),now); }
    @BeforeEach void setup() {
        client=MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI","mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin&serverSelectionTimeoutMS=5000"));
        mongo=new MongoTemplate(client,"billing_payment_test_"+UUID.randomUUID().toString().replace("-",""));
        var doc=new CobrancaDocument(id.toString(),id.toString(),id.toString(),new BigDecimal("51.00"),"BRL","PENDENTE",now,now);
        mongo.insert(new OrcamentoDocument(id.toString(),id.toString(),List.of(),now,"APROVADO",id.toString(),doc));
        charges=new CobrancaService(new CobrancaMongoGateway(mongo,new OrcamentoPersistenceMapper()),clock);
        checkouts=new CheckoutMongoGateway(mongo); checkouts.salvarSeAusente(id,new Checkout("ORDTEST","url"));
        payments=new PagamentoMongoGateway(mongo); inbox=new NotificacaoPagamentoMongo(mongo,clock);
        usecase=new ConfirmarPagamentoUseCase(provider,charges,checkouts,payments,clock);
        when(provider.consultar("ORDTEST")).thenReturn(valid());
    }
    @AfterEach void cleanup() { if(mongo!=null) mongo.getDb().drop(); if(client!=null) client.close(); }
    PagamentoInboxWorker worker() { return new PagamentoInboxWorker(mongo,usecase,clock); }
    Document notification() { return mongo.findById("ORDTEST",Document.class,"notificacoes_pagamento"); }
    Document receipt() { return mongo.findById(id.toString(),Document.class,"orcamentos").get("pagamento",Document.class); }
    @Test void processaDepoisDeReiniciarSemDuplicarComprovante() {
        inbox.registrar("ORDTEST");
        assertEquals(Cobranca.Status.PENDENTE,charges.buscar(id).status());
        assertTrue(worker().processarProxima()); assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status());
        var first=receipt();
        inbox.registrar("ORDTEST");
        assertTrue(new PagamentoInboxWorker(mongo,usecase,Clock.fixed(now.plusSeconds(10),ZoneOffset.UTC)).processarProxima());
        assertEquals(first,receipt()); assertFalse(notification().getBoolean("pendente")); assertFalse(worker().processarProxima());
        assertThrows(br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException.class,
            ()->new CriarCheckoutUseCase(charges,checkouts,mock(ProvedorCheckout.class)).executar(id));
    }
    @Test void indisponibilidadeReagendaERetomaSemPerderAviso() {
        when(provider.consultar("ORDTEST")).thenThrow(new CheckoutIndisponivelException()).thenReturn(valid());
        inbox.registrar("ORDTEST"); assertFalse(worker().processarProxima());
        assertTrue(notification().getBoolean("pendente")); assertFalse(worker().processarProxima());
        assertEquals("CheckoutIndisponivelException",notification().getString("ultimoErro"));
        assertTrue(new PagamentoInboxWorker(mongo,usecase,Clock.fixed(now.plusSeconds(31),ZoneOffset.UTC)).processarProxima());
        assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status()); assertFalse(notification().containsKey("ultimoErro"));
    }
    @Test void novaNotificacaoDuranteConsultaPermanecePendente() {
        inbox.registrar("ORDTEST");
        when(provider.consultar("ORDTEST")).thenAnswer(inv->{ inbox.registrar("ORDTEST"); return valid(); });
        assertTrue(worker().processarProxima()); assertTrue(notification().getBoolean("pendente"));
        when(provider.consultar("ORDTEST")).thenReturn(valid());
        assertTrue(worker().processarProxima()); assertFalse(notification().getBoolean("pendente"));
    }
    @Test void leaseEvitaProcessamentoSimultaneoEExpiradaPodeRetomar() throws Exception {
        inbox.registrar("ORDTEST"); var entered=new CountDownLatch(1); var release=new CountDownLatch(1);
        when(provider.consultar("ORDTEST")).thenAnswer(inv->{ entered.countDown(); assertTrue(release.await(10,TimeUnit.SECONDS)); return valid(); });
        try(var executor=Executors.newSingleThreadExecutor()) {
            var future=executor.submit(()->worker().processarProxima());
            try { assertTrue(entered.await(10,TimeUnit.SECONDS)); assertFalse(worker().processarProxima()); }
            finally { release.countDown(); }
            assertTrue(future.get(10,TimeUnit.SECONDS));
        }
        inbox.registrar("ORDTEST");
        mongo.updateFirst(Query.query(Criteria.where("_id").is("ORDTEST")),new Update().set("leaseAte",Date.from(now.minusSeconds(1))).set("leaseToken","crashed"),"notificacoes_pagamento");
        assertTrue(worker().processarProxima());
    }
    @Test void confirmacaoConcorrenteEReentregaAposFalhaDeAckSaoIdempotentes() throws Exception {
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new ArrayList<Callable<Void>>();
            for(int i=0;i<8;i++) tasks.add(()->{ payments.confirmar(valid(),now); return null; });
            for(var task:executor.invokeAll(tasks)) task.get();
        }
        var first=receipt(); inbox.registrar("ORDTEST"); assertTrue(worker().processarProxima()); assertEquals(first,receipt());
        assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status());
    }
    @Test void dadosDivergentesNaoAlteramCobranca() {
        var wrong=new OrderPagamento("ORDTEST",id,"processed","accredited","BRL",new BigDecimal("52.00"),new BigDecimal("52.00"),now);
        when(provider.consultar("ORDTEST")).thenReturn(wrong);
        inbox.registrar("ORDTEST"); assertFalse(worker().processarProxima());
        assertEquals(Cobranca.Status.PENDENTE,charges.buscar(id).status()); assertNull(receipt());
        assertEquals("PagamentoDivergenteException",notification().getString("ultimoErro"));
        assertThrows(PagamentoDivergenteException.class,()->payments.confirmar(wrong,now));
    }
    @Test void statusNaoCreditadoNaoMarcaPagaNemRegridePagamentoConfirmado() {
        var pending=new OrderPagamento("ORDTEST",id,"created","created","BRL",new BigDecimal("51"),BigDecimal.ZERO,now);
        when(provider.consultar("ORDTEST")).thenReturn(pending); inbox.registrar("ORDTEST"); assertTrue(worker().processarProxima());
        assertEquals(Cobranca.Status.PENDENTE,charges.buscar(id).status());
        assertThrows(PagamentoDivergenteException.class,()->payments.confirmar(pending,now));
        payments.confirmar(valid(),now); inbox.registrar("ORDTEST"); assertTrue(worker().processarProxima());
        assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status());
    }

    @Test void webhookAteConsultaHttpEMongoEGetDaCobranca() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/orders/ORDTEST",exchange->{
            String response="{\"id\":\"ORDTEST\",\"type\":\"online\",\"external_reference\":\""+id+"\","
                +"\"status\":\"processed\",\"status_detail\":\"accredited\",\"currency\":\"BRL\","
                +"\"total_amount\":\"51.00\",\"total_paid_amount\":\"51.00\",\"last_updated_date\":\"2026-10-23T12:00:00Z\"}";
            var bytes=response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        }); server.start();
        try {
            var adapter=new MercadoPagoConsulta(java.net.http.HttpClient.newHttpClient(),
                java.net.URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/orders"),"fake-token");
            var realUsecase=new ConfirmarPagamentoUseCase(adapter,charges,checkouts,payments,clock);
            var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new br.com.techchallenge.mecanica.billing.presentation.controller.MercadoPagoWebhookController(new AssinaturaMercadoPago("test-webhook-secret"),inbox),
                new br.com.techchallenge.mecanica.billing.presentation.controller.CobrancaController(charges)).build();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/webhooks/mercado-pago")
                .param("data.id","ORDTEST").param("type","order").header("x-request-id","req-123")
                .header("x-signature","ts=1792771200000,v1=d7d4a079007bb15d26776c312126f94000cac75c0f426bbb8cf7e6cbdff6c991"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            assertEquals(Cobranca.Status.PENDENTE,charges.buscar(id).status());
            assertTrue(new PagamentoInboxWorker(mongo,realUsecase,clock).processarProxima());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/cobrancas/"+id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("PAGA"));
        } finally { server.stop(0); }
    }

    Document paymentEvent() { return mongo.findById(id.toString(),Document.class,"orcamentos").get("pagamentoEvento",Document.class); }
    @Test void pagamentoEEventoAtomicosRepeticaoNaoRearmaPublicacao() {
        assertNull(paymentEvent()); payments.confirmar(valid(),now);
        assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status()); assertTrue(paymentEvent().getBoolean("pendente"));
        var events=new java.util.ArrayList<br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoConfirmado>();
        var worker=new br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoOutboxWorker(mongo,events::add,clock);
        assertTrue(worker.publicarProximo()); assertFalse(worker.publicarProximo());
        var published=paymentEvent(); payments.confirmar(valid(),now.plusSeconds(100));
        assertEquals(published,paymentEvent()); assertFalse(worker.publicarProximo()); assertEquals(1,events.size());
        var event=events.getFirst(); assertEquals(id,event.eventId()); assertEquals(id,event.cobrancaId());
        assertEquals(id,event.ordemId()); assertEquals(id,event.reparoId()); assertEquals(now,event.confirmadoEm());
        assertEquals(0,new BigDecimal("51").compareTo(event.valor())); assertEquals("BRL",event.moeda());
    }
    @Test void falhaNoBrokerPreservaPagamentoERetentaMesmoEvento() {
        payments.confirmar(valid(),now);
        var failing=new br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoOutboxWorker(mongo,
            event->{throw new IllegalStateException("broker offline");},clock);
        assertFalse(failing.publicarProximo()); assertEquals(Cobranca.Status.PAGA,charges.buscar(id).status());
        assertTrue(paymentEvent().getBoolean("pendente")); assertFalse(paymentEvent().containsKey("publicadoEm"));
        var events=new java.util.ArrayList<br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoConfirmado>();
        var later=new br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoOutboxWorker(mongo,events::add,
            Clock.fixed(now.plusSeconds(31),ZoneOffset.UTC));
        assertTrue(later.publicarProximo()); assertEquals(id,events.getFirst().eventId()); assertEquals(now,events.getFirst().confirmadoEm());
    }
    @Test void doisPublicadoresNaoPegamMesmaLeaseAtiva() throws Exception {
        payments.confirmar(valid(),now); var entered=new CountDownLatch(1); var release=new CountDownLatch(1);
        var worker=new br.com.techchallenge.mecanica.billing.infrastructure.messaging.PagamentoOutboxWorker(mongo,event->{
            entered.countDown(); try { assertTrue(release.await(10,TimeUnit.SECONDS)); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        },clock);
        try(var executor=Executors.newSingleThreadExecutor()) {
            var first=executor.submit(worker::publicarProximo);
            try { assertTrue(entered.await(10,TimeUnit.SECONDS)); assertFalse(worker.publicarProximo()); }
            finally { release.countDown(); }
            assertTrue(first.get(10,TimeUnit.SECONDS));
        }
    }
}
