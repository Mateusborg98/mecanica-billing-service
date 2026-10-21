package br.com.techchallenge.mecanica.billing.presentation.exception;

import br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.techchallenge.mecanica.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException.class)
    public ProblemDetail tratarCheckout(br.com.techchallenge.mecanica.billing.application.exception.CheckoutIndisponivelException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,exception.getMessage());
    }

    @ExceptionHandler(br.com.techchallenge.mecanica.billing.application.exception.CobrancaNaoEncontradaException.class)
    public ProblemDetail tratarCobrancaNaoEncontrada(br.com.techchallenge.mecanica.billing.application.exception.CobrancaNaoEncontradaException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,exception.getMessage());
    }

    @ExceptionHandler(ConflitoOrcamentoException.class)
    public ProblemDetail tratarConflito(ConflitoOrcamentoException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OrcamentoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(
            OrcamentoNaoEncontradoException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(
            RegraNegocioException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
    }
}
