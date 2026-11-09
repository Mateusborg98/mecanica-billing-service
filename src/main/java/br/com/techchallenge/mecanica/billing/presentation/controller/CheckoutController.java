package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.usecase.CriarCheckoutUseCase;
@RestController
@RequestMapping("/cobrancas")
public class CheckoutController {
    private final CriarCheckoutUseCase useCase;
    public CheckoutController(CriarCheckoutUseCase useCase) { this.useCase=useCase; }
    @PostMapping("/{orcamentoId}/checkout")
    public Checkout criar(@PathVariable UUID orcamentoId) { return useCase.executar(orcamentoId); }
}
