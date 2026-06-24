package calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CalculatorServer {

    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
    private final Calculator calculator = new Calculator();

    public static void main(String[] args) throws IOException {
        new CalculatorServer().start();
    }

    void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handleRoot);
        server.setExecutor(null);
        server.start();
        System.out.println("Serveur démarré sur http://localhost:" + PORT);
    }

    void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/calculate".equals(path)) {
            handleRequest(exchange);
        } else {
            try {
                setCorsHeaders(exchange);
                sendJson(exchange, 404, "{\"error\":\"Route introuvable.\"}");
            } finally {
                exchange.close();
            }
        }
    }

    void handleRequest(HttpExchange exchange) throws IOException {
        try {
            setCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET, OPTIONS");
                sendJson(exchange, 405, "{\"error\":\"Méthode non autorisée. Utiliser GET.\"}");
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> query = parseQuery(uri.getRawQuery());

            String op = query.get("operation");
            String aStr = query.get("a");
            String bStr = query.get("b");

            if (op == null || aStr == null || bStr == null) {
                sendJson(exchange, 400, "{\"error\":\"Paramètres attendus : operation, a, b\"}");
                return;
            }

            double a;
            double b;
            try {
                a = Double.parseDouble(aStr);
                b = Double.parseDouble(bStr);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"Les paramètres a et b doivent être des nombres.\"}");
                return;
            }

            double result;
            try {
                switch (op) {
                    case "add":
                        result = calculator.add(a, b);
                        break;
                    case "subtract":
                        result = calculator.subtract(a, b);
                        break;
                    case "multiply":
                        result = calculator.multiply(a, b);
                        break;
                    case "divide":
                        result = calculator.divide(a, b);
                        break;
                    default:
                        sendJson(exchange, 400, "{\"error\":\"Opération inconnue. Utiliser : add, subtract, multiply, divide\"}");
                        return;
                }
            } catch (ArithmeticException e) {
                sendJson(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
                return;
            }

            String json = String.format(
                    "{\"operation\":\"%s\",\"a\":%s,\"b\":%s,\"result\":%s}",
                    op, formatNumber(a), formatNumber(b), formatNumber(result)
            );
            sendJson(exchange, 200, json);
        } finally {
            exchange.close();
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String param : rawQuery.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0], parts[1]);
            } else if (parts.length == 1) {
                map.put(parts[0], "");
            }
        }
        return map;
    }

    static String formatNumber(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "null";
        }
        if (d == (long) d) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
