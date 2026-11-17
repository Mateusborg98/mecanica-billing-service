package br.com.techchallenge.mecanica.webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/** Local development ingress. Does not log bodies, signatures or credentials. */
public final class WebhookProxy implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private final int upstreamPort;

    WebhookProxy(int port, int upstreamPort) throws IOException {
        this.upstreamPort = upstreamPort;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 64);
        executor = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64));
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }
    int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            var uri = exchange.getRequestURI();
            if (!"POST".equals(exchange.getRequestMethod()) || uri.isAbsolute() || uri.getRawAuthority() != null
                || !"/webhooks/mercado-pago".equals(uri.getRawPath()) || uri.getRawQuery() == null || uri.toString().length() > 2048) {
                reply(exchange, 404); return;
            }
            var headers = exchange.getRequestHeaders();
            var lengths = headers.getOrDefault("Content-Length", List.of());
            if (headers.containsKey("Transfer-Encoding") || lengths.size() > 1) { reply(exchange, 400); return; }
            var length = lengths.isEmpty() ? "0" : lengths.getFirst();
            if (!length.matches("[0-9]{1,6}") || Integer.parseInt(length) > 65536) { reply(exchange, 413); return; }
            for (var name : List.of("x-signature", "x-request-id")) {
                var values = headers.getOrDefault(name, List.of());
                if (values.size() != 1 || values.getFirst().isBlank() || values.getFirst().length() > 1024) { reply(exchange, 401); return; }
            }
            HttpURLConnection upstream = null;
            try {
                int size = Integer.parseInt(length);
                byte[] body = exchange.getRequestBody().readNBytes(size);
                if (body.length != size) { reply(exchange, 400); return; }
                upstream = (HttpURLConnection) URI.create("http://127.0.0.1:" + upstreamPort + uri).toURL().openConnection();
                upstream.setConnectTimeout(5000); upstream.setReadTimeout(5000); upstream.setInstanceFollowRedirects(false);
                upstream.setRequestMethod("POST"); upstream.setDoOutput(true); upstream.setFixedLengthStreamingMode(body.length);
                upstream.setRequestProperty("Content-Type", "application/json");
                for (var name : List.of("x-signature", "x-request-id")) upstream.setRequestProperty(name, headers.getFirst(name));
                try (var output = upstream.getOutputStream()) { output.write(body); }
                int status = upstream.getResponseCode();
                reply(exchange, Set.of(200, 201, 400, 401, 404, 413, 503).contains(status) ? status : 502);
            } catch (IOException ex) {
                reply(exchange, 503);
            } finally { if (upstream != null) upstream.disconnect(); }
        }
    }

    private static void reply(HttpExchange exchange, int status) throws IOException {
        exchange.getResponseHeaders().set("Connection", "close");
        exchange.sendResponseHeaders(status, -1);
    }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }

    public static void main(String[] args) throws Exception {
        // JDK HttpServer limits prevent a slow request from holding a worker indefinitely.
        System.setProperty("sun.net.httpserver.maxReqTime", "5");
        System.setProperty("sun.net.httpserver.maxRspTime", "10");
        var proxy = new WebhookProxy(8084, 8082);
        Runtime.getRuntime().addShutdownHook(new Thread(proxy::close));
        System.out.println("Webhook ingress: 127.0.0.1:8084 -> Billing 127.0.0.1:8082");
    }
}
