package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;

class OrcamentoGatewayTest {
    private final OrcamentoMongoRepository repository = mock(OrcamentoMongoRepository.class);
    private final MongoTemplate template = mock(MongoTemplate.class);
    private final OrcamentoGatewayImpl gateway = new OrcamentoGatewayImpl(repository, new OrcamentoPersistenceMapper(), template);
    private Orcamento novo() {
        return new Orcamento(UUID.randomUUID(), List.of(new ItemOrcamento(UUID.randomUUID(),
                TipoItemOrcamento.SERVICO, "Reparo", 1, BigDecimal.TEN)), Instant.now());
    }
    @Test void deveInserirSemSobrescrever() {
        gateway.salvar(novo());
        verify(repository).insert(any(OrcamentoDocument.class));
        verify(repository, never()).save(any(OrcamentoDocument.class));
        verifyNoInteractions(template);
    }
    @Test void deveTraduzirDuplicidadeParaConflito() {
        when(repository.insert(any(OrcamentoDocument.class))).thenThrow(new DuplicateKeyException("duplicado"));
        assertThrows(ConflitoOrcamentoException.class, () -> gateway.salvar(novo()));
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void deveCondicionarDecisaoAoEstadoPendente(boolean aprovar) {
        var orcamento = novo();
        if (aprovar) orcamento.aprovar(); else orcamento.recusar();
        when(template.updateFirst(any(Query.class), any(Update.class), eq(OrcamentoDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        gateway.salvar(orcamento);
        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        var update = org.mockito.ArgumentCaptor.forClass(Update.class);
        verify(template).updateFirst(query.capture(), update.capture(), eq(OrcamentoDocument.class));
        assertEquals(orcamento.getId().toString(), query.getValue().getQueryObject().getString("_id"));
        assertEquals("AGUARDANDO_APROVACAO", query.getValue().getQueryObject().getString("status"));
        assertEquals(orcamento.getStatus().name(),
                update.getValue().getUpdateObject().get("$set", org.bson.Document.class).getString("status"));
        verifyNoInteractions(repository);
    }
    @Test void deveRecusarAtualizacaoObsoleta() {
        var orcamento = novo(); orcamento.aprovar();
        when(template.updateFirst(any(Query.class), any(Update.class), eq(OrcamentoDocument.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThrows(ConflitoOrcamentoException.class, () -> gateway.salvar(orcamento));
    }
}
