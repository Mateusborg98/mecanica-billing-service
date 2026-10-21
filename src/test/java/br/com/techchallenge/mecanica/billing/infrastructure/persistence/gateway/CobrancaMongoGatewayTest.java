package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.dao.DataAccessResourceFailureException;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
class CobrancaMongoGatewayTest {
    private final UUID reparo=UUID.randomUUID(), ordem=UUID.randomUUID();
    private final UUID id=UUID.nameUUIDFromBytes(("mecanica:diagnostico:v1:"+reparo).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private final Instant agora=Instant.parse("2026-10-20T15:00:00Z");
    private final ConclusaoReparoInput input=new ConclusaoReparoInput(reparo,reparo,ordem,id,agora);
    private final MongoTemplate mongo=mock(MongoTemplate.class);
    private final OrcamentoPersistenceMapper mapper=new OrcamentoPersistenceMapper();
    private final CobrancaMongoGateway gateway=new CobrancaMongoGateway(mongo,mapper);
    private OrcamentoDocument doc(String status) {
        return mapper.toDocument(Orcamento.reconstituir(id,ordem,List.of(new ItemOrcamento(UUID.randomUUID(),TipoItemOrcamento.SERVICO,"Servico",2,new BigDecimal("25.50"))),agora.minusSeconds(10),StatusOrcamento.valueOf(status))).comReparo(reparo);
    }
    private OrcamentoDocument comCobranca(OrcamentoDocument d) {
        return new OrcamentoDocument(d.id(),d.ordemId(),d.itens(),d.criadoEm(),d.status(),d.reparoId(),
                new CobrancaDocument(reparo.toString(),reparo.toString(),ordem.toString(),new BigDecimal("51.00"),"BRL","PENDENTE",agora,agora));
    }
    @Test void concorrenteQuePerdeUpdateDeveRecuperarValorPersistidoSemSobrescrever() {
        var aprovado=doc("APROVADO"); var salvo=comCobranca(aprovado);
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(aprovado,salvo);
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(OrcamentoDocument.class))).thenReturn(null);
        var result=gateway.criarOuRecuperar(input,agora.plusSeconds(5));
        assertEquals(agora,result.criadaEm()); assertEquals(new BigDecimal("51.00"),result.valorTotal());
        assertEquals(Cobranca.Status.PENDENTE,result.status());
        var update=org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(any(Query.class),update.capture(),any(FindAndModifyOptions.class),eq(OrcamentoDocument.class));
        var pending=(CobrancaDocument)update.getValue().getUpdateObject().get("$set",org.bson.Document.class).get("cobranca");
        assertEquals(new BigDecimal("51.00"),pending.valorTotal());
    }
    @Test void reentregaDeveLerCobrancaExistenteSemAtualizar() {
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(comCobranca(doc("APROVADO")));
        assertEquals(gateway.criarOuRecuperar(input,agora.plusSeconds(99)),gateway.buscar(id).orElseThrow());
        verify(mongo,never()).findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(OrcamentoDocument.class));
    }
    @Test void falhaDeBancoDevePropagarParaRetry() {
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(doc("APROVADO"));
        when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(OrcamentoDocument.class)))
                .thenThrow(new DataAccessResourceFailureException("Offline"));
        assertThrows(DataAccessResourceFailureException.class,()->gateway.criarOuRecuperar(input,agora));
    }
    @Test void ausenciaDepoisDeUpdatePerdidoNaoDeveSerTratadaComoSucesso() {
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(doc("APROVADO"),null);
        assertThrows(IllegalStateException.class,()->gateway.criarOuRecuperar(input,agora));
    }
    @Test void deveRejeitarOrcamentoAusentePendenteOuSemCorrelacao() {
        assertTrue(gateway.buscar(id).isEmpty());
        assertThrows(IllegalArgumentException.class,()->gateway.criarOuRecuperar(input,agora));
        var aprovado=doc("APROVADO");
        for(var d:List.of(doc("AGUARDANDO_APROVACAO"),doc("RECUSADO"),new OrcamentoDocument(aprovado.id(),aprovado.ordemId(),aprovado.itens(),aprovado.criadoEm(),"APROVADO"))) {
            when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(d);
            assertThrows(IllegalArgumentException.class,()->gateway.criarOuRecuperar(input,agora));
            assertTrue(gateway.buscar(id).isEmpty());
        }
    }
    @Test void deveRejeitarDataAnteriorEDuplicataConflitante() {
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(doc("APROVADO"));
        assertThrows(IllegalArgumentException.class,()->gateway.criarOuRecuperar(new ConclusaoReparoInput(reparo,reparo,ordem,id,agora.minusSeconds(20)),agora));
        when(mongo.findById(id.toString(),OrcamentoDocument.class)).thenReturn(comCobranca(doc("APROVADO")));
        assertThrows(IllegalArgumentException.class,()->gateway.criarOuRecuperar(new ConclusaoReparoInput(reparo,reparo,ordem,id,agora.plusSeconds(20)),agora));
    }
}
