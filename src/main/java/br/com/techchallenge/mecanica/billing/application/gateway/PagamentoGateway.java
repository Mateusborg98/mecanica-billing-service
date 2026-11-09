package br.com.techchallenge.mecanica.billing.application.gateway;
import java.time.Instant;
import br.com.techchallenge.mecanica.billing.application.dto.OrderPagamento;
public interface PagamentoGateway { void confirmar(OrderPagamento order, Instant confirmadoEm); }
