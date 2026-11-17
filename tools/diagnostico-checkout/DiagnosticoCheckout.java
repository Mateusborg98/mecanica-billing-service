import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Manual sandbox diagnostic. Never changes the Billing database or confirms payment. */
public class DiagnosticoCheckout {
    private static final String ORIGINAL = "ORDTST01M7JQT3KPM7FQN4GZSDW2WV3B";
    private static final JsonMapper JSON = new JsonMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static void main(String[] args) throws Exception {
        String mode = args.length == 1 ? args[0] : "--preview";
        if (!Set.of("--preview", "--consultar", "--criar-teste").contains(mode)) {
            throw new IllegalArgumentException("Use --preview, --consultar ou --criar-teste");
        }
        var payload = new LinkedHashMap<String, Object>();
        payload.put("type", "online");
        payload.put("processing_mode", "manual");
        payload.put("total_amount", "50.00");
        payload.put("items", List.of(Map.of("title", "Reparo da oficina", "quantity", 1, "unit_price", "50.00")));
        if (mode.equals("--preview")) {
            System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            System.out.println("Preview local. Nenhuma requisicao enviada. A criacao adiciona referencia e idempotencia exclusivas do diagnostico.");
            return;
        }
        String token = System.getenv("MERCADO_PAGO_ACCESS_TOKEN");
        if (token == null || token.isBlank()) throw new IllegalStateException("Configure MERCADO_PAGO_ACCESS_TOKEN nesta sessao; nao informe no chat.");
        var original = send("GET", "/" + ORIGINAL, token, null, null);
        summary(original);
        if (!mode.equals("--criar-teste")) return;
        if (!ORIGINAL.equals(original.path("id").asString()) || !ORIGINAL.startsWith("ORDTST")) {
            throw new IllegalStateException("Consulta da order de teste nao corresponde ao registro esperado.");
        }
        Path keyFile = Path.of("target", "diagnostico-checkout-id.txt");
        Files.createDirectories(keyFile.getParent());
        if (!Files.exists(keyFile)) Files.writeString(keyFile, UUID.randomUUID().toString(), StandardOpenOption.CREATE_NEW);
        String key = UUID.fromString(Files.readString(keyFile).trim()).toString();
        payload.put("external_reference", "diagnostico-" + key);
        System.out.println("Criando/reconsultando operacao isolada com chave persistida; nao vinculada a OS.");
        var created = send("POST", "", token, JSON.writeValueAsString(payload), key);
        summary(created);
        if (!created.path("id").asString("").startsWith("ORDTST")) {
            throw new IllegalStateException("Resposta nao identifica order de teste. Nao prossiga para pagamento.");
        }
        var url = URI.create(created.path("checkout_url").asString(""));
        if (!"https".equals(url.getScheme()) || !"www.mercadopago.com.br".equals(url.getHost()) || url.getUserInfo() != null) {
            throw new IllegalStateException("checkout_url inesperada.");
        }
        System.out.println("checkoutUrl=" + url);
    }

    private static JsonNode send(String method, String path, String token, String body, String key) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("https://api.mercadopago.com/v1/orders" + path))
            .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + token).header("Accept", "application/json");
        if (body != null) builder.header("Content-Type", "application/json");
        if (key != null) builder.header("X-Idempotency-Key", key);
        var response = HTTP.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        System.out.println(method + " HTTP " + response.statusCode() + " requestId=" + response.headers().firstValue("x-request-id").orElse("ausente"));
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("API recusou a requisicao. Corpo e token omitidos.");
        return JSON.readTree(response.body());
    }

    private static void summary(JsonNode node) {
        var safe = JSON.createObjectNode();
        for (String field : List.of("id", "status", "status_detail", "type", "processing_mode", "capture_mode", "total_amount", "total_paid_amount", "currency", "country_code", "external_reference", "created_date", "expiration_time")) {
            if (node.has(field)) safe.set(field, node.get(field));
        }
        safe.put("quantidadePagamentos", node.path("transactions").path("payments").size());
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(safe));
    }
}