package br.com.techchallenge.mecanica.billing.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;

class BuscarOrcamentoPorIdUseCaseTest {

    @Test
    void deveRetornarOrcamentoEncontrado() {
        var gateway = new GatewayDeTeste();
        var orcamento = novoOrcamento();
        gateway.salvar(orcamento);

        var useCase = new BuscarOrcamentoPorIdUseCase(gateway);

        var resultado = useCase.executar(orcamento.getId());

        assertSame(orcamento, resultado);
        assertEquals(orcamento.getId(), gateway.idConsultado);
    }

    @Test
    void deveInformarQuandoOrcamentoNaoExistir() {
        var gateway = new GatewayDeTeste();
        var useCase = new BuscarOrcamentoPorIdUseCase(gateway);
        UUID id = UUID.randomUUID();

        assertThrows(
                OrcamentoNaoEncontradoException.class,
                () -> useCase.executar(id));

        assertEquals(id, gateway.idConsultado);
    }

    @Test
    void deveRejeitarIdAusenteSemConsultarGateway() {
        var gateway = new GatewayDeTeste();
        var useCase = new BuscarOrcamentoPorIdUseCase(gateway);

        assertThrows(
                RegraNegocioException.class,
                () -> useCase.executar(null));

        assertEquals(0, gateway.consultas);
    }

    @Test
    void devePropagarFalhaDePersistencia() {
        var falha = new IllegalStateException("Banco indisponível");

        var gateway = new GatewayDeTeste() {
            @Override
            public Optional<Orcamento> buscarPorId(UUID id) {
                throw falha;
            }
        };

        var useCase = new BuscarOrcamentoPorIdUseCase(gateway);

        var recebida = assertThrows(
                IllegalStateException.class,
                () -> useCase.executar(UUID.randomUUID()));

        assertSame(falha, recebida);
    }

    private Orcamento novoOrcamento() {
        var item = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.SERVICO,
                "Alinhamento",
                1,
                new BigDecimal("120.00"));

        return new Orcamento(
                UUID.randomUUID(),
                List.of(item),
                Instant.parse("2026-10-09T12:00:00Z"));
    }

    private static class GatewayDeTeste implements OrcamentoGateway {

        private Orcamento salvo;
        private UUID idConsultado;
        private int consultas;

        @Override
        public void salvar(Orcamento orcamento) {
            this.salvo = orcamento;
        }

        @Override
        public Optional<Orcamento> buscarPorId(UUID id) {
            this.idConsultado = id;
            this.consultas++;

            return Optional.ofNullable(salvo)
                    .filter(orcamento -> orcamento.getId().equals(id));
        }
    }
}
