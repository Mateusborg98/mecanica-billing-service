"""Local test ingress: expose only the Mercado Pago webhook, never the Billing API."""
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit

class WebhookProxy(BaseHTTPRequestHandler):
    upstream_port = 8082
    def setup(self):
        super().setup()
        self.connection.settimeout(5)
    def log_message(self, *_):
        pass  # Do not record signatures or request bodies.
    def reply(self, status):
        self.send_response(status)
        self.send_header("Content-Length", "0")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True
    def do_GET(self):
        self.reply(404)
    do_HEAD = do_GET
    do_PUT = do_GET
    do_DELETE = do_GET
    do_OPTIONS = do_GET
    do_PATCH = do_GET
    def do_POST(self):
        if self.requestline.split()[1] != self.path or len(self.path) > 2048 or not self.path.startswith("/webhooks/mercado-pago?"):
            self.reply(404)
            return
        if urlsplit(self.path).path != "/webhooks/mercado-pago":
            self.reply(404)
            return
        lengths = self.headers.get_all("Content-Length", [])
        if self.headers.get("Transfer-Encoding") or len(lengths) > 1:
            self.reply(400)
            return
        value = lengths[0] if lengths else "0"
        if not value.isdigit() or len(value) > 6 or int(value) > 65536:
            self.reply(413)
            return
        headers = {"Content-Type": "application/json"}
        for name in ("x-signature", "x-request-id"):
            values = self.headers.get_all(name, [])
            if len(values) != 1 or not values[0] or len(values[0]) > 1024:
                self.reply(401)
                return
            headers[name] = values[0]
        conn = http.client.HTTPConnection("127.0.0.1", self.upstream_port, timeout=5)
        try:
            payload = self.rfile.read(int(value))
            if len(payload) != int(value):
                self.reply(400)
                return
            conn.request("POST", self.path, body=payload, headers=headers)
            response = conn.getresponse()
            # No upstream headers/body or redirects are exposed.
            self.reply(response.status if response.status in (200, 201, 400, 401, 404, 413, 503) else 502)
        except (OSError, http.client.HTTPException):
            self.reply(503)
        finally:
            conn.close()

if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8084), WebhookProxy)
    print("Webhook ingress: 127.0.0.1:8084 -> Billing 127.0.0.1:8082", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
