package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
@RestController
@RequestMapping("/orcamentos/{id}/aprovacao")
public class AprovacaoPublicaController {
    private final BuscarOrcamentoPorIdUseCase buscar;
    private final AprovarOrcamentoUseCase aprovar;
    private final RecusarOrcamentoUseCase recusar;
    public AprovacaoPublicaController(BuscarOrcamentoPorIdUseCase buscar,AprovarOrcamentoUseCase aprovar,RecusarOrcamentoUseCase recusar) {
        this.buscar=buscar;this.aprovar=aprovar;this.recusar=recusar;
    }
    @GetMapping(produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(@PathVariable UUID id) {
        var o=buscar.executar(id);var itens=new StringBuilder();
        for(var i:o.getItens()) itens.append("<tr><td>").append(HtmlUtils.htmlEscape(i.descricao())).append("</td><td>")
            .append(i.quantidade()).append("</td><td>R$ ").append(i.calcularSubtotal().toPlainString()).append("</td></tr>");
        String estado=switch(o.getStatus()) {case AGUARDANDO_APROVACAO -> "Aguardando sua decisão";case APROVADO -> "Aprovado";case RECUSADO -> "Recusado";case CANCELADO -> "Cancelado";};
        String acoes=o.getStatus()==StatusOrcamento.AGUARDANDO_APROVACAO
            ? "<form method='post'><button name='decisao' value='APROVAR'>Aprovar orçamento</button> <button name='decisao' value='RECUSAR'>Recusar orçamento</button></form>"
            : "<p>A decisão deste orçamento já foi registrada.</p>";
        String html="<!doctype html><html lang='pt-BR'><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'>"
            +"<title>Orçamento da oficina</title><body><main><h1>Seu orçamento</h1><p>"+estado+"</p>"
            +"<table><caption>Peças e serviços</caption><thead><tr><th>Descrição</th><th>Quantidade</th><th>Subtotal</th></tr></thead><tbody>"
            +itens+"</tbody></table><p><strong>Total: R$ "+o.calcularTotal().toPlainString()+"</strong></p>"
            +"<p>A aprovação autoriza o reparo. O pagamento ocorre após a conclusão, antes da entrega.</p>"+acoes+"</main></body></html>";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer")
            .header("Content-Security-Policy","default-src 'none'; form-action 'self'; frame-ancestors 'none'")
            .contentType(MediaType.TEXT_HTML).body(html);
    }
    @PostMapping public ResponseEntity<Void> decidir(@PathVariable UUID id,@RequestParam String decisao) {
        switch(decisao) {case "APROVAR" -> aprovar.executar(id);case "RECUSAR" -> recusar.executar(id);
            default -> throw new RegraNegocioException("Escolha aprovar ou recusar.");}
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/orcamentos/"+id+"/aprovacao")).build();
    }
}
