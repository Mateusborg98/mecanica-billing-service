package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.OrcamentoDiagnosticoMongoGateway;
class OrcamentoDiagnosticoMongoGatewayTest {
    private Orcamento novo() {
        return new Orcamento(UUID.randomUUID(), List.of(new ItemOrcamento(UUID.randomUUID(), TipoItemOrcamento.SERVICO,
                "Revisão", 1, BigDecimal.TEN)), Instant.now());
    }
    @Test void deveInserirNovoOrcamento() {
        var mongo = mock(MongoTemplate.class); var mapper = new OrcamentoPersistenceMapper(); var candidato = novo();
        assertSame(candidato, new OrcamentoDiagnosticoMongoGateway(mongo, mapper).inserirOuRecuperar(candidato));
        verify(mongo).insert(mapper.toDocument(candidato));
    }
    @Test void reenvioNaoDeveSobrescreverDecisaoExistente() {
        var mongo = mock(MongoTemplate.class); var mapper = new OrcamentoPersistenceMapper(); var candidato = novo();
        var aprovado = Orcamento.reconstituir(candidato.getId(), candidato.getOrdemId(), candidato.getItens(), candidato.getCriadoEm(), StatusOrcamento.APROVADO);
        when(mongo.insert(any(OrcamentoDocument.class))).thenThrow(new DuplicateKeyException("duplicado"));
        when(mongo.findById(candidato.getId().toString(), OrcamentoDocument.class)).thenReturn(mapper.toDocument(aprovado));
        var resultado = new OrcamentoDiagnosticoMongoGateway(mongo, mapper).inserirOuRecuperar(candidato);
        assertEquals(StatusOrcamento.APROVADO, resultado.getStatus());
        verify(mongo, never()).save(any(OrcamentoDocument.class));
    }
    @Test void deveRecusarConteudoDiferente() {
        var mongo = mock(MongoTemplate.class); var mapper = new OrcamentoPersistenceMapper(); var candidato = novo();
        var item = candidato.getItens().getFirst();
        var diferente = Orcamento.reconstituir(candidato.getId(), candidato.getOrdemId(), List.of(new ItemOrcamento(
                item.referenciaId(), item.tipo(), item.descricao(), 2, item.valorUnitario())), candidato.getCriadoEm(), StatusOrcamento.AGUARDANDO_APROVACAO);
        when(mongo.insert(any(OrcamentoDocument.class))).thenThrow(new DuplicateKeyException("duplicado"));
        when(mongo.findById(candidato.getId().toString(), OrcamentoDocument.class)).thenReturn(mapper.toDocument(diferente));
        assertThrows(IllegalArgumentException.class, () -> new OrcamentoDiagnosticoMongoGateway(mongo, mapper).inserirOuRecuperar(candidato));
    }
    @Test void naoDeveOcultarDuplicidadeSemDocumentoRecuperavel() {
        var mongo = mock(MongoTemplate.class);
        when(mongo.insert(any(OrcamentoDocument.class))).thenThrow(new DuplicateKeyException("duplicado"));
        assertThrows(DuplicateKeyException.class, () -> new OrcamentoDiagnosticoMongoGateway(mongo, new OrcamentoPersistenceMapper()).inserirOuRecuperar(novo()));
    }
}
