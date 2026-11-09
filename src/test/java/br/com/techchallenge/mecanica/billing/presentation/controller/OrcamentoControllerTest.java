package br.com.techchallenge.mecanica.billing.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.CriarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.RecusarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.presentation.exception.GlobalExceptionHandler;
import br.com.techchallenge.mecanica.billing.presentation.mapper.OrcamentoPresentationMapper;

class OrcamentoControllerTest {

        private MockMvc mockMvc;
        private GatewayEmMemoria gateway;

        @BeforeEach
        void configurar() {
                gateway = new GatewayEmMemoria();

                var clock = Clock.fixed(
                                Instant.parse("2026-10-09T12:00:00Z"),
                                ZoneOffset.UTC);

                var buscarUseCase = new BuscarOrcamentoPorIdUseCase(gateway);

                var controller = new OrcamentoController(
                                new CriarOrcamentoUseCase(gateway, clock),
                                buscarUseCase,
                                new AprovarOrcamentoUseCase(buscarUseCase, gateway),
                                new RecusarOrcamentoUseCase(buscarUseCase, gateway),
                                new OrcamentoPresentationMapper());
                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void deveResponder409QuandoDecisaoConcorrenteVencer() throws Exception {
                var aprovar = org.mockito.Mockito.mock(AprovarOrcamentoUseCase.class);
                var id = UUID.randomUUID();
                org.mockito.Mockito.when(aprovar.executar(id)).thenThrow(
                        new br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException(id));
                var controller = new OrcamentoController(
                        org.mockito.Mockito.mock(CriarOrcamentoUseCase.class),
                        org.mockito.Mockito.mock(BuscarOrcamentoPorIdUseCase.class), aprovar,
                        org.mockito.Mockito.mock(RecusarOrcamentoUseCase.class), new OrcamentoPresentationMapper());
                MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
                        .build().perform(post("/orcamentos/" + id + "/aprovar"))
                        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        }

        @Test
        void deveCriarEConsultarPeloEnderecoRetornado() throws Exception {
                var resposta = mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonValido()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status")
                                                .value("AGUARDANDO_APROVACAO"))
                                .andExpect(jsonPath("$.valorTotal").value(51.0))
                                .andExpect(jsonPath("$.itens[0].subtotal").value(51.0))
                                .andReturn()
                                .getResponse();

                String location = resposta.getHeader("Location");

                assertNotNull(location);
                assertTrue(location.startsWith("/orcamentos/"));
                assertEquals(1, gateway.registros.size());

                String id = location.substring("/orcamentos/".length());

                mockMvc.perform(get(location))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id))
                                .andExpect(jsonPath("$.ordemId")
                                                .value("11111111-1111-4111-8111-111111111111"))
                                .andExpect(jsonPath("$.status")
                                                .value("AGUARDANDO_APROVACAO"))
                                .andExpect(jsonPath("$.valorTotal").value(51.0));
        }

        @Test
        void deveRetornar404QuandoOrcamentoNaoExistir() throws Exception {
                mockMvc.perform(get("/orcamentos/{id}", UUID.randomUUID()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        void deveRetornar400QuandoIdNaoForUuid() throws Exception {
                mockMvc.perform(get("/orcamentos/id-invalido"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void deveRejeitarOrcamentoSemItens() throws Exception {
                String json = """
                                {
                                  "ordemId": "11111111-1111-4111-8111-111111111111",
                                  "itens": []
                                }
                                """;

                mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());

                assertTrue(gateway.registros.isEmpty());
        }

        @Test
        void deveRejeitarQuantidadeZeroSemSalvar() throws Exception {
                String json = jsonValido().replace(
                                "\"quantidade\": 2",
                                "\"quantidade\": 0");

                mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());

                assertTrue(gateway.registros.isEmpty());
        }

        @Test
        void deveRejeitarTipoDeItemDesconhecido() throws Exception {
                String json = jsonValido().replace("\"PECA\"", "\"OUTRO\"");

                mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());

                assertTrue(gateway.registros.isEmpty());
        }

        @Test
        void deveRejeitarJsonMalformado() throws Exception {
                mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{"))
                                .andExpect(status().isBadRequest());

                assertTrue(gateway.registros.isEmpty());
        }

        private String jsonValido() {
                return """
                                {
                                  "ordemId": "11111111-1111-4111-8111-111111111111",
                                  "itens": [
                                    {
                                      "referenciaId": "22222222-2222-4222-8222-222222222222",
                                      "tipo": "PECA",
                                      "descricao": "Filtro",
                                      "quantidade": 2,
                                      "valorUnitario": 25.50
                                    }
                                  ]
                                }
                                """;
        }

        private static class GatewayEmMemoria implements OrcamentoGateway {

                private final Map<UUID, Orcamento> registros = new HashMap<>();

                @Override
                public void salvar(Orcamento orcamento) {
                        registros.put(orcamento.getId(), copiar(orcamento));
                }

                @Override
                public Optional<Orcamento> buscarPorId(UUID id) {
                        return Optional.ofNullable(registros.get(id))
                                        .map(this::copiar);
                }

                private Orcamento copiar(Orcamento orcamento) {
                        return Orcamento.reconstituir(
                                        orcamento.getId(),
                                        orcamento.getOrdemId(),
                                        orcamento.getItens(),
                                        orcamento.getCriadoEm(),
                                        orcamento.getStatus());
                }
        }

        @Test
        void deveAprovarOrcamentoEConsultarNovoStatus() throws Exception {
                String location = criarOrcamentoPelaApi();

                mockMvc.perform(post(location + "/aprovar"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("APROVADO"));

                mockMvc.perform(get(location))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("APROVADO"));
        }

        @Test
        void deveRecusarOrcamentoEConsultarNovoStatus() throws Exception {
                String location = criarOrcamentoPelaApi();

                mockMvc.perform(post(location + "/recusar"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RECUSADO"));

                mockMvc.perform(get(location))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RECUSADO"));
        }

        @Test
        void deveImpedirRecusaAposAprovacao() throws Exception {
                String location = criarOrcamentoPelaApi();

                mockMvc.perform(post(location + "/aprovar"))
                                .andExpect(status().isOk());

                mockMvc.perform(post(location + "/recusar"))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(get(location))
                                .andExpect(jsonPath("$.status").value("APROVADO"));
        }

        @Test
        void deveRetornar404AoAprovarOrcamentoInexistente() throws Exception {
                mockMvc.perform(post(
                                "/orcamentos/{id}/aprovar",
                                UUID.randomUUID()))
                                .andExpect(status().isNotFound());
        }

        private String criarOrcamentoPelaApi() throws Exception {
                var resposta = mockMvc.perform(post("/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonValido()))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse();

                String location = resposta.getHeader("Location");
                assertNotNull(location);
                return location;
        }
}
