package br.com.techchallenge.mecanica.billing.application.gateway;
import br.com.techchallenge.mecanica.billing.application.dto.OrderPagamento;
public interface ConsultaPagamento { OrderPagamento consultar(String orderId); }
