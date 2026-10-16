package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;

@EnabledIfEnvironmentVariable(named = "RUN_MONGO_TESTS", matches = "true")
class OrcamentoGatewayIntegrationTest {

    private MongoClient client;
    private MongoTemplate template;
    private OrcamentoMongoRepository repository;
    private OrcamentoGatewayImpl gateway;
    private UUID idCriado;

    @BeforeEach
    void configurar() {
        client = MongoClients.create(System.getenv().getOrDefault("TEST_MONGO_URI",
                "mongodb://billing_local:billing_local_password"
                        + "@localhost:27018"
                        + "/?authSource=admin&serverSelectionTimeoutMS=5000"));

        template = new MongoTemplate(
                client,
                "mecanica_billing_test");

        repository = new MongoRepositoryFactory(template)
                .getRepository(OrcamentoMongoRepository.class);

        gateway = new OrcamentoGatewayImpl(
                repository,
                new OrcamentoPersistenceMapper(), template);
    }

    @AfterEach
    void limpar() {
        try {
            if (repository != null && idCriado != null) {
                repository.deleteById(idCriado.toString());
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Test
    void deveSalvarERecuperarOrcamentoAprovado() {
        var item = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "Filtro",
                2,
                new BigDecimal("25.50"));

        var original = new Orcamento(
                UUID.randomUUID(),
                List.of(item),
                Instant.parse("2026-10-09T12:00:00Z"));

        gateway.salvar(original);
        original.aprovar();
        idCriado = original.getId();

        gateway.salvar(original);

        var recuperado = gateway.buscarPorId(idCriado)
                .orElseThrow();

        assertEquals(original.getId(), recuperado.getId());
        assertEquals(original.getOrdemId(), recuperado.getOrdemId());
        assertEquals(original.getCriadoEm(), recuperado.getCriadoEm());
        assertEquals(original.getItens(), recuperado.getItens());
        assertEquals(StatusOrcamento.APROVADO, recuperado.getStatus());
        assertEquals(
                new BigDecimal("51.00"),
                recuperado.calcularTotal());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void devePreservarPrimeiraDecisao(boolean aprovarPrimeiro) {
        var original = new Orcamento(UUID.randomUUID(), List.of(new ItemOrcamento(
                UUID.randomUUID(), TipoItemOrcamento.SERVICO, "Reparo", 1, BigDecimal.TEN)), Instant.now());
        idCriado = original.getId();
        gateway.salvar(original);
        var primeiro = gateway.buscarPorId(idCriado).orElseThrow();
        var segundo = gateway.buscarPorId(idCriado).orElseThrow();
        if (aprovarPrimeiro) { primeiro.aprovar(); segundo.recusar(); }
        else { primeiro.recusar(); segundo.aprovar(); }
        gateway.salvar(primeiro);
        org.junit.jupiter.api.Assertions.assertThrows(
                br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException.class,
                () -> gateway.salvar(segundo));
        assertEquals(primeiro.getStatus(), gateway.buscarPorId(idCriado).orElseThrow().getStatus());
    }

    @Test
    void deveAceitarUmaUnicaDecisaoEmDuasThreads() throws Exception {
        var original = new Orcamento(UUID.randomUUID(), List.of(new ItemOrcamento(
                UUID.randomUUID(), TipoItemOrcamento.SERVICO, "Reparo", 1, BigDecimal.TEN)), Instant.now());
        idCriado = original.getId(); gateway.salvar(original);
        var aprovacao = gateway.buscarPorId(idCriado).orElseThrow(); aprovacao.aprovar();
        var recusa = gateway.buscarPorId(idCriado).orElseThrow(); recusa.recusar();
        var largada = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> tentarSalvar(aprovacao, largada));
            var segunda = executor.submit(() -> tentarSalvar(recusa, largada));
            largada.countDown();
            boolean aprovou = primeira.get(10, java.util.concurrent.TimeUnit.SECONDS);
            boolean recusou = segunda.get(10, java.util.concurrent.TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertNotEquals(aprovou, recusou);
            assertEquals(aprovou ? StatusOrcamento.APROVADO : StatusOrcamento.RECUSADO,
                    gateway.buscarPorId(idCriado).orElseThrow().getStatus());
        }
    }

    private boolean tentarSalvar(Orcamento orcamento, java.util.concurrent.CountDownLatch largada) throws Exception {
        largada.await();
        try { gateway.salvar(orcamento); return true; }
        catch (br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException exception) {
            return false;
        }
    }

    @Test
    void deveReceberDiagnosticoRepetidoSemSobrescreverAprovacao() {
        var adapter = new OrcamentoDiagnosticoMongoGateway(template, new OrcamentoPersistenceMapper());
        var useCase = new br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase(adapter);
        var reparoId = UUID.randomUUID(); var ordemId = UUID.randomUUID();
        var itens = List.of(new ItemOrcamento(UUID.randomUUID(), TipoItemOrcamento.SERVICO, "Inspeção", 1, BigDecimal.TEN));
        var criado = useCase.executar(reparoId, ordemId, Instant.now(), itens);
        idCriado = criado.getId();
        criado.aprovar(); gateway.salvar(criado);
        var repetido = useCase.executar(reparoId, ordemId, Instant.now(), itens);
        assertEquals(idCriado, repetido.getId()); assertEquals(StatusOrcamento.APROVADO, repetido.getStatus());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> useCase.executar(reparoId, UUID.randomUUID(), Instant.now(), itens));
    }

    @Test
    void deveRetornarVazioQuandoOrcamentoNaoExistir() {
        var resultado = gateway.buscarPorId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }
}
