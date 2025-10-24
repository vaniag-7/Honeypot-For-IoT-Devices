// HoneypotServer.java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HoneypotServer {
    private HttpServer server;
    private LogListener listener;

    public interface LogListener {
        void onLogReceived(String srcIp, String device, String proto, String action, String details);
    }

    public HoneypotServer(int port, LogListener listener) throws IOException {
        this.listener = listener;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/iot", this::handleRequest);
        server.setExecutor(null);
    }

    public void start() {
        server.start();
        System.out.println("Honeypot server running on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("Honeypot server stopped.");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        // Add CORS headers (so browser iot.html can call)
        Headers respHeaders = exchange.getResponseHeaders();
        respHeaders.add("Access-Control-Allow-Origin", "*");
        respHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        respHeaders.add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String srcIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        String query = exchange.getRequestURI().getQuery(); // e.g., device=Smart+Light&action=heartbeat
        String method = exchange.getRequestMethod();

        InputStream is = exchange.getRequestBody();
        byte[] bodyBytes = is.readAllBytes();
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        String details = "Method=" + method + ", Query=" + (query == null ? "-" : query) + ", Body=" + (body.isEmpty() ? "-" : body);

        // parse device param
        String device = "unknown-device";
        if (query != null) {
            for (String part : query.split("&")) {
                if (part.startsWith("device=")) {
                    device = URLDecoder.decode(part.substring("device=".length()), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        // notify UI listener
        if (listener != null) {
            try {
                listener.onLogReceived(srcIp, device, "HTTP", "REQUEST", details);
            } catch (Exception ex) {
                System.err.println("Listener error: " + ex.getMessage());
            }
        }

        String response = "OK - Honeypot received";
        byte[] resp = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }
}
