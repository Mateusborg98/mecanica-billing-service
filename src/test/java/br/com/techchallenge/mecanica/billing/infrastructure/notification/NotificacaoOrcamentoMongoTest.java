package br.com.techchallenge.mecanica.billing.infrastructure.notification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.mongodb.client.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.gateway.*;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.application.exception.*;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
@EnabledIfEnvironmentVariable(named="RUN_MONGO_TESTS",matches="true")
class NotificacaoOrcamentoMongoTest {
    MongoClient client;MongoTemplate mongo;NotificacaoOrcamentoMongoOutbox outbox;
    BuscarOrcamentoPorIdUseCase buscar;Orcamento o;EnviarOrcamentoUseCase enviar;
    final Instant agora=Instant.parse("2026-11-09T12:00:00Z");
    @BeforeEach void setup() {
        client=MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI","mongodb://billing_local:billing_local_password@localhost:27018/?authSource=admin&serverSelectionTimeoutMS=5000"));
        mongo=new MongoTemplate(client,"billing_email_test_"+UUID.randomUUID().toString().replace("-",""));
        outbox=new NotificacaoOrcamentoMongoOutbox(mongo);
        o=new Orcamento(UUID.randomUUID(),List.of(new ItemOrcamento(UUID.randomUUID(),TipoItemOrcamento.SERVICO,"Reparo",1,new BigDecimal("100.00"))),agora);
        var gateway=mock(OrcamentoGateway.class);when(gateway.buscarPorId(o.getId())).thenReturn(Optional.of(o));
        buscar=new BuscarOrcamentoPorIdUseCase(gateway);
        enviar=new EnviarOrcamentoUseCase(buscar,outbox,"https://oficina.example",Clock.fixed(agora,ZoneOffset.UTC));
    }
    @AfterEach void cleanup(){if(mongo!=null)mongo.getDb().drop();if(client!=null)client.close();}
    void agendar(){enviar.executar(o.getId(),"cliente@example.test");}
    @Test void repeticaoNaoDuplicaEOutroDestinatarioConflita() {
        agendar();agendar();assertEquals(1,mongo.getCollection("envios_orcamento").countDocuments());
        assertEquals("PENDENTE",outbox.buscar(o.getId()).status());
        assertThrows(ConflitoOrcamentoException.class,()->enviar.executar(o.getId(),"outro@example.test"));
        assertThrows(OrcamentoNaoEncontradoException.class,()->outbox.buscar(UUID.randomUUID()));
    }
    @Test void leaseExpiraEMarcacaoAntigaNaoConfirmaOutraTentativa() {
        agendar();var primeira=outbox.reservar(agora);assertNotNull(primeira);assertNull(outbox.reservar(agora));
        var segunda=outbox.reservar(agora.plusSeconds(61));assertNotNull(segunda);assertNotEquals(primeira.token(),segunda.token());
        outbox.concluir(primeira,"ENVIADO");assertEquals("PENDENTE",outbox.buscar(o.getId()).status());
        outbox.concluir(segunda,"ENVIADO");assertEquals("ENVIADO",outbox.buscar(o.getId()).status());
        assertNull(outbox.reservar(agora.plusSeconds(200)));assertEquals(2,outbox.buscar(o.getId()).tentativas());
    }
    @Test void falhaSmtpPermanecePendenteERetomaSemPerderMensagem() {
        agendar();var count=new AtomicInteger();
        EnvioEmailGateway email=(destino,texto)->{if(count.getAndIncrement()==0)throw new IllegalStateException("SMTP indisponível");assertEquals("cliente@example.test",destino);assertTrue(texto.contains("/orcamentos/"+o.getId()+"/aprovacao"));};
        var worker=new EnvioOrcamentoWorker(outbox,email,buscar,Clock.fixed(agora,ZoneOffset.UTC));
        assertFalse(worker.enviarProximo());assertFalse(worker.enviarProximo());
        assertEquals("PENDENTE",outbox.buscar(o.getId()).status());
        assertTrue(new EnvioOrcamentoWorker(outbox,email,buscar,Clock.fixed(agora.plusSeconds(31),ZoneOffset.UTC)).enviarProximo());
        assertEquals("ENVIADO",outbox.buscar(o.getId()).status());assertEquals(2,count.get());
    }
    @Test void decisaoAnteriorAoEnvioCancelaNotificacaoPendente() {
        agendar();o.recusar();var email=mock(EnvioEmailGateway.class);
        assertTrue(new EnvioOrcamentoWorker(outbox,email,buscar,Clock.fixed(agora,ZoneOffset.UTC)).enviarProximo());
        assertEquals("CANCELADO",outbox.buscar(o.getId()).status());verifyNoInteractions(email);
    }
    @Test void duasInstanciasNaoReservamAMesmaMensagemAoMesmoTempo() throws Exception {
        agendar();try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->outbox.reservar(agora));var b=pool.submit(()->outbox.reservar(agora));
            int reservadas=(a.get()==null?0:1)+(b.get()==null?0:1);assertEquals(1,reservadas);
        }
    }
    @Test void validaDestinatarioEstadoEUrlPublica() {
        for(String destino:Arrays.asList(null,"","a\nb","a\rb"))assertThrows(RegraNegocioException.class,()->enviar.executar(o.getId(),destino));
        o.aprovar();assertThrows(RegraNegocioException.class,this::agendar);
        for(String url:List.of("http://externo.example","https://user@oficina.example","https://oficina.example?q=1","https://oficina.example#x","relative"))
            assertThrows(IllegalArgumentException.class,()->new EnviarOrcamentoUseCase(buscar,outbox,url,Clock.systemUTC()));
        assertDoesNotThrow(()->new EnviarOrcamentoUseCase(buscar,outbox,"http://localhost:8082/",Clock.systemUTC()));
    }
}
