package br.com.techchallenge.mecanica.webhook;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

class WebhookProxyTest {
    private HttpServer upstream;
    private WebhookProxy proxy;
    private HttpClient client;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private volatile int status;
    private record Call(String path, Map<String, List<String>> headers, String body) { }
    private static final String PATH = "/webhooks/mercado-pago?data.id=ORD1&type=order";

    @BeforeEach void start() throws Exception {
        status = 200;
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/", exchange -> {
            try (exchange) {
                calls.add(new Call(exchange.getRequestURI().toString(), new TreeMap<>(exchange.getRequestHeaders()),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + upstream.getAddress().getPort() + "/private");
                exchange.getResponseHeaders().set("X-Secret", "not-forwarded");
                byte[] body = "private upstream body".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length); exchange.getResponseBody().write(body);
            }
        });
        upstream.start(); proxy = new WebhookProxy(0, upstream.getAddress().getPort());
        client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @AfterEach void stop() { proxy.close(); upstream.stop(0); client.close(); }

    private HttpResponse<String> request(String method, String path, boolean signed) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + proxy.port() + path))
            .timeout(java.time.Duration.ofSeconds(8)).method(method, HttpRequest.BodyPublishers.ofString("{}"));
        if (signed) request.header("x-signature", "ts=1,v1=fake").header("x-request-id", "req-1");
        request.header("Authorization", "must-not-forward");
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private int raw(String headers) throws Exception {
        try (var socket = new Socket("127.0.0.1", proxy.port())) {
            socket.setSoTimeout(8000);
            socket.getOutputStream().write(("POST " + PATH + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n" + headers + "\r\n{}").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            return Integer.parseInt(reader.readLine().split(" ")[1]);
        }
    }

    @Test void forwardsOnlyExactWebhookAndRequiredHeaders() throws Exception {
        assertEquals(200, request("POST", PATH, true).statusCode());
        assertEquals(1, calls.size()); assertEquals(PATH, calls.getFirst().path()); assertEquals("{}", calls.getFirst().body());
        var headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER); headers.putAll(calls.getFirst().headers());
        assertEquals(List.of("ts=1,v1=fake"), headers.get("x-signature"));
        assertEquals(List.of("req-1"), headers.get("x-request-id")); assertFalse(headers.containsKey("Authorization"));
    }

    @Test void administrativePathsAndOtherMethodsAreBlocked() throws Exception {
        for (var path : List.of("/orcamentos", "/cobrancas/1/checkout", "/actuator/health", "/webhooks/mercado-pago/../orcamentos",
                "//webhooks/mercado-pago?x=1", "/webhooks/mercado-pago", "/webhooks/%6dercado-pago?x=1"))
            assertEquals(404, request("POST", path, true).statusCode(), path);
        for (var method : List.of("GET", "PUT", "DELETE", "OPTIONS", "PATCH")) assertEquals(404, request(method, PATH, true).statusCode());
        assertTrue(calls.isEmpty());
    }

    @Test void rejectsMissingOrDuplicateSignature() throws Exception {
        assertEquals(401, request("POST", PATH, false).statusCode());
        assertEquals(401, raw("Content-Length: 2\r\nx-signature: a\r\nx-signature: b\r\nx-request-id: req\r\n"));
        assertTrue(calls.isEmpty());
    }

    @Test void rejectsOversizedBodiesAndAmbiguousFraming() throws Exception {
        assertEquals(413, raw("Content-Length: 65537\r\n"));
        assertEquals(400, raw("Transfer-Encoding: chunked\r\n"));
        assertEquals(400, raw("Content-Length: 2\r\nContent-Length: 3\r\n"));
        assertTrue(calls.isEmpty());
    }

    @Test void preservesAllowedStatusWithoutLeakingBodyOrHeaders() throws Exception {
        for (int code : List.of(200, 201, 400, 401, 404, 413, 503)) {
            status = code; var result = request("POST", PATH, true);
            assertEquals(code, result.statusCode()); assertEquals("", result.body());
            assertTrue(result.headers().firstValue("X-Secret").isEmpty()); assertTrue(result.headers().firstValue("Location").isEmpty());
        }
    }

    @Test void neverFollowsUpstreamRedirect() throws Exception {
        status = 302; assertEquals(502, request("POST", PATH, true).statusCode()); assertEquals(1, calls.size());
    }

    @Test void unavailableUpstreamReturns503() throws Exception {
        upstream.stop(0); assertEquals(503, request("POST", PATH, true).statusCode());
    }
}
