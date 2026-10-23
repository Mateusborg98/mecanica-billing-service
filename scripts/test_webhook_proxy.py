import http.client
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from webhook_proxy import WebhookProxy

class Upstream(BaseHTTPRequestHandler):
    calls = []
    status = 200
    def log_message(self, *_): pass
    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        self.calls.append((self.path, dict(self.headers), body))
        self.send_response(self.status)
        self.end_headers()

class ProxyTest(unittest.TestCase):
    def setUp(self):
        Upstream.calls = []
        Upstream.status = 200
        self.upstream = ThreadingHTTPServer(("127.0.0.1", 0), Upstream)
        handler = type("TestProxy", (WebhookProxy,), {"upstream_port": self.upstream.server_port})
        self.proxy = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        for server in (self.upstream, self.proxy):
            threading.Thread(target=server.serve_forever, daemon=True).start()
    def tearDown(self):
        for server in (self.upstream, self.proxy):
            server.shutdown()
            server.server_close()
    def request(self, method="POST", path="/webhooks/mercado-pago?data.id=ORD1&type=order", headers=None):
        conn = http.client.HTTPConnection("127.0.0.1", self.proxy.server_port, timeout=5)
        try:
            conn.request(method, path, body=b"{}", headers=headers or {})
            return conn.getresponse().status
        finally:
            conn.close()
    def test_only_exact_webhook_post_is_forwarded(self):
        headers = {"x-signature": "ts=1,v1=fake", "x-request-id": "req-1", "Authorization": "must-not-forward"}
        self.assertEqual(200, self.request(headers=headers))
        path, forwarded, body = Upstream.calls[0]
        self.assertEqual("/webhooks/mercado-pago?data.id=ORD1&type=order", path)
        self.assertEqual("ts=1,v1=fake", forwarded["x-signature"])
        self.assertNotIn("Authorization", forwarded)
        self.assertEqual(b"{}", body)
    def test_administrative_routes_and_other_methods_are_blocked(self):
        for path in ("/orcamentos", "/cobrancas/1/checkout", "/actuator/health", "/webhooks/mercado-pago/../orcamentos", "//webhooks/mercado-pago?x=1"):
            self.assertEqual(404, self.request(path=path))
        self.assertEqual(404, self.request(method="GET"))
        self.assertEqual([], Upstream.calls)
    def test_missing_signature_and_large_body_are_rejected(self):
        self.assertEqual(401, self.request())
        self.assertEqual(413, self.request(headers={"Content-Length": "65537"}))
        self.assertEqual(400, self.request(headers={"Transfer-Encoding": "chunked"}))
        self.assertEqual([], Upstream.calls)
    def test_upstream_status_is_preserved_without_redirect(self):
        headers = {"x-signature": "fake", "x-request-id": "req"}
        Upstream.status = 401
        self.assertEqual(401, self.request(headers=headers))
        Upstream.status = 302
        self.assertEqual(502, self.request(headers=headers))

if __name__ == "__main__": unittest.main()
