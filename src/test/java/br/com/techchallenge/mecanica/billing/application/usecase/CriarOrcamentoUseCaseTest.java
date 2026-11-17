package br.com.techchallenge.mecanica.billing.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.billing.application.dto.CriarOrcamentoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.ItemOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.orcamento.TipoItemOrcamento;

class CriarOrcamentoUseCaseTest {

        private static final Instant AGORA = Instant.parse("2026-10-06T12:00:00Z");

        private static final Clock CLOCK = Clock.fixed(AGORA, ZoneOffset.UTC);

        @Test
        void deveCriarESalvarOrcamento() {
                var gateway = new GatewayDeTeste();
                var useCase = new CriarOrcamentoUseCase(gateway, CLOCK);
                var input = entradaValida();

                var resultado = useCase.executar(input);

                assertNotNull(resultado.getId());
                assertEquals(input.ordemId(), resultado.getOrdemId());
                assertEquals(input.itens(), resultado.getItens());
                assertEquals(AGORA, resultado.getCriadoEm());
                assertEquals(
                                StatusOrcamento.AGUARDANDO_APROVACAO,
                                resultado.getStatus());
                assertEquals(
                                new BigDecimal("50.00"),
                                resultado.calcularTotal());

                assertEquals(1, gateway.chamadas);
                assertSame(resultado, gateway.salvo);
        }

        @Test
        void naoDeveSalvarQuandoEntradaEstiverAusente() {
                var gateway = new GatewayDeTeste();
                var useCase = new CriarOrcamentoUseCase(gateway, CLOCK);

                assertThrows(
                                RegraNegocioException.class,
                                () -> useCase.executar(null));

                assertEquals(0, gateway.chamadas);
        }

        @Test
        void naoDeveSalvarQuandoOrcamentoViolarRegraDeDominio() {
                var gateway = new GatewayDeTeste();
                var useCase = new CriarOrcamentoUseCase(gateway, CLOCK);

                var input = new CriarOrcamentoInput(
                                UUID.randomUUID(),
                                List.of());

                assertThrows(
                                RegraNegocioException.class,
                                () -> useCase.executar(input));

                assertEquals(0, gateway.chamadas);
        }

        @Test
        void devePropagarFalhaAoSalvar() {
                var falha = new IllegalStateException(
                                "Persistência indisponível");

                OrcamentoGateway gateway = new GatewayDeTeste() {
                        @Override
                        public void salvar(Orcamento orcamento) {
                                throw falha;
                        }
                };

                var useCase = new CriarOrcamentoUseCase(gateway, CLOCK);

                var recebida = assertThrows(
                                IllegalStateException.class,
                                () -> useCase.executar(entradaValida()));

                assertSame(falha, recebida);
        }

        private CriarOrcamentoInput entradaValida() {
                var item = new ItemOrcamento(
                                UUID.randomUUID(),
                                TipoItemOrcamento.PECA,
                                "Filtro",
                                2,
                                new BigDecimal("25.00"));

                return new CriarOrcamentoInput(
                                UUID.randomUUID(),
                                List.of(item));
        }

        private static class GatewayDeTeste implements OrcamentoGateway {

                private Orcamento salvo;
                private int chamadas;

                @Override
                public Optional<Orcamento> buscarPorId(UUID id) {
                        return Optional.ofNullable(salvo)
                                        .filter(orcamento -> orcamento.getId().equals(id));
                }

                @Override
                public void salvar(Orcamento orcamento) {
                        this.salvo = orcamento;
                        this.chamadas++;
                }
        }
}
