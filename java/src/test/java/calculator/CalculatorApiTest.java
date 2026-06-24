package calculator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("API /calculate")
class CalculatorApiTest {

    private static HttpServer server;
    private static int port;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void setUp() throws IOException {
        CalculatorServer calcServer = new CalculatorServer();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", calcServer::handleRoot);
        server.setExecutor(null);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void tearDown() {
        server.stop(0);
    }

    private static Response request(String path) throws Exception {
        return request(path, "GET");
    }

    private static Response request(String path, String method) throws Exception {
        long start = System.currentTimeMillis();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path));
        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else if ("OPTIONS".equalsIgnoreCase(method)) {
            builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - start;

        JsonObject body = null;
        if (response.body() != null && !response.body().isEmpty()) {
            body = JsonParser.parseString(response.body()).getAsJsonObject();
        }
        return new Response(response.statusCode(), response.headers().map(), body, duration);
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        @DisplayName("valid request responds in less than 100ms")
        void testPerformanceValid() throws Exception {
            Response res = request("/calculate?operation=add&a=1&b=2");
            assertTrue(res.duration < 100);
        }

        @Test
        @DisplayName("400 error responds in less than 100ms")
        void testPerformanceError() throws Exception {
            Response res = request("/calculate?operation=add&a=2");
            assertTrue(res.duration < 100);
        }
    }

    @Nested
    @DisplayName("Response headers")
    class ResponseHeaders {

        @Test
        @DisplayName("should have correct headers on 200 response")
        void testHeaders200() throws Exception {
            Response res = request("/calculate?operation=add&a=1&b=2");
            assertEquals("application/json; charset=utf-8", res.headers.get("Content-Type").get(0));
            assertEquals("*", res.headers.get("Access-Control-Allow-Origin").get(0));
        }

        @Test
        @DisplayName("should have correct headers on 400 response")
        void testHeaders400() throws Exception {
            Response res = request("/calculate?operation=add&a=2");
            assertEquals("application/json; charset=utf-8", res.headers.get("Content-Type").get(0));
            assertEquals("*", res.headers.get("Access-Control-Allow-Origin").get(0));
        }

        @Test
        @DisplayName("should have correct headers on 404 response")
        void testHeaders404() throws Exception {
            Response res = request("/unknown");
            assertEquals("application/json; charset=utf-8", res.headers.get("Content-Type").get(0));
            assertEquals("*", res.headers.get("Access-Control-Allow-Origin").get(0));
        }
    }

    @Nested
    @DisplayName("OPTIONS /calculate - CORS preflight")
    class OptionsPreflight {

        @Test
        @DisplayName("should return 204 without body")
        void testOptions204() throws Exception {
            Response res = request("/calculate", "OPTIONS");
            assertEquals(204, res.status);
            assertNull(res.body);
        }

        @Test
        @DisplayName("should have Access-Control-Allow-Origin *")
        void testOptionsOrigin() throws Exception {
            Response res = request("/calculate", "OPTIONS");
            assertEquals("*", res.headers.get("Access-Control-Allow-Origin").get(0));
        }

        @Test
        @DisplayName("should have Access-Control-Allow-Methods containing GET")
        void testOptionsMethods() throws Exception {
            Response res = request("/calculate", "OPTIONS");
            assertTrue(res.headers.get("Access-Control-Allow-Methods").get(0).contains("GET"));
        }
    }

    @Nested
    @DisplayName("GET /calculate - nominal cases")
    class NominalCases {

        @ParameterizedTest
        @CsvSource({
                "add, 2, 3, 5",
                "subtract, 10, 4, 6",
                "multiply, 6, 7, 42",
                "divide, 20, 5, 4",
                "add, -5, -3, -8",
                "subtract, -5, -3, -2",
                "multiply, -3, -4, 12",
                "divide, -10, -2, 5"
        })
        @DisplayName("operation(a, b) should return expected result")
        void testNominal(String operation, double a, double b, double expected) throws Exception {
            Response res = request(String.format("/calculate?operation=%s&a=%s&b=%s", operation, a, b));
            assertEquals(200, res.status);
            assertEquals(operation, res.body.get("operation").getAsString());
            assertEquals(a, res.body.get("a").getAsDouble());
            assertEquals(b, res.body.get("b").getAsDouble());
            assertEquals(expected, res.body.get("result").getAsDouble());
        }

        @Test
        @DisplayName("decimal division: divide(10, 3) ≈ 3.333")
        void testDecimalDivision() throws Exception {
            Response res = request("/calculate?operation=divide&a=10&b=3");
            assertEquals(200, res.status);
            assertEquals(3.333, res.body.get("result").getAsDouble(), 1e-3);
        }

        @Test
        @DisplayName("decimals in query string: add(1.5, 2.5) === 4")
        void testDecimalQuery() throws Exception {
            Response res = request("/calculate?operation=add&a=1.5&b=2.5");
            assertEquals(200, res.status);
            assertEquals(4, res.body.get("result").getAsDouble());
        }

        @Test
        @DisplayName("JSON 200 contract: body contains operation, a, b, result and no error")
        void testJsonContract() throws Exception {
            Response res = request("/calculate?operation=multiply&a=3&b=4");
            assertTrue(res.body.has("operation"));
            assertTrue(res.body.has("a"));
            assertTrue(res.body.has("b"));
            assertTrue(res.body.has("result"));
            assertFalse(res.body.has("error"));
        }
    }

    @Nested
    @DisplayName("Method not allowed")
    class MethodNotAllowed {

        @Test
        @DisplayName("POST should return 405 with error")
        void testPost405() throws Exception {
            Response res = request("/calculate", "POST");
            assertEquals(405, res.status);
            assertTrue(res.body.has("error"));
        }

        @Test
        @DisplayName("POST should have Allow header containing GET")
        void testPostAllowHeader() throws Exception {
            Response res = request("/calculate", "POST");
            assertTrue(res.headers.get("Allow").get(0).contains("GET"));
        }

        @Test
        @DisplayName("PUT should return 405")
        void testPut405() throws Exception {
            Response res = request("/calculate", "PUT");
            assertEquals(405, res.status);
        }
    }

    @Nested
    @DisplayName("GET /calculate - 400 errors")
    class BadRequests {

        @Test
        @DisplayName("missing b: status 400, error contains 'Paramètres attendus'")
        void testMissingB() throws Exception {
            Response res = request("/calculate?operation=add&a=2");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("Paramètres attendus"));
        }

        @Test
        @DisplayName("missing a: status 400, error contains 'Paramètres attendus'")
        void testMissingA() throws Exception {
            Response res = request("/calculate?operation=add&b=2");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("Paramètres attendus"));
        }

        @Test
        @DisplayName("non-numeric a: status 400, error contains 'doivent être des nombres'")
        void testNonNumericA() throws Exception {
            Response res = request("/calculate?operation=add&a=abc&b=3");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("doivent être des nombres"));
        }

        @Test
        @DisplayName("non-numeric b: status 400, error contains 'doivent être des nombres'")
        void testNonNumericB() throws Exception {
            Response res = request("/calculate?operation=add&a=3&b=abc");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("doivent être des nombres"));
        }

        @Test
        @DisplayName("division by zero: status 400, exact error message")
        void testDivisionByZero() throws Exception {
            Response res = request("/calculate?operation=divide&a=10&b=0");
            assertEquals(400, res.status);
            assertEquals("Division par zéro impossible.", res.body.get("error").getAsString());
        }

        @Test
        @DisplayName("unknown operation: status 400, error contains 'Opération inconnue'")
        void testUnknownOperation() throws Exception {
            Response res = request("/calculate?operation=modulo&a=10&b=3");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("Opération inconnue"));
        }

        @Test
        @DisplayName("missing operation: status 400, error contains 'Paramètres attendus'")
        void testMissingOperation() throws Exception {
            Response res = request("/calculate?a=5&b=3");
            assertEquals(400, res.status);
            assertTrue(res.body.get("error").getAsString().contains("Paramètres attendus"));
        }

        @Test
        @DisplayName("JSON error contract: body has error and no result")
        void testErrorContract() throws Exception {
            Response res = request("/calculate?operation=add&a=2");
            assertTrue(res.body.has("error"));
            assertFalse(res.body.has("result"));
        }
    }

    @Nested
    @DisplayName("GET - other routes")
    class OtherRoutes {

        @Test
        @DisplayName("unknown route /unknown: status 404, error 'Route introuvable.'")
        void testUnknownRoute() throws Exception {
            Response res = request("/unknown");
            assertEquals(404, res.status);
            assertEquals("Route introuvable.", res.body.get("error").getAsString());
        }

        @Test
        @DisplayName("root /: status 404, body has error")
        void testRoot() throws Exception {
            Response res = request("/");
            assertEquals(404, res.status);
            assertTrue(res.body.has("error"));
        }

        @Test
        @DisplayName("trailing slash /calculate/: status 404, body has error")
        void testTrailingSlash() throws Exception {
            Response res = request("/calculate/");
            assertEquals(404, res.status);
            assertTrue(res.body.has("error"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("large value: add(1e308, 1e308) returns null (Infinity)")
        void testLargeValue() throws Exception {
            Response res = request("/calculate?operation=add&a=1e308&b=1e308");
            assertEquals(200, res.status);
            assertTrue(res.body.get("result").isJsonNull());
        }

        @Test
        @DisplayName("a=-0: add(-0, 5) returns result 5, a 0")
        void testNegativeZero() throws Exception {
            Response res = request("/calculate?operation=add&a=-0&b=5");
            assertEquals(200, res.status);
            assertEquals(5, res.body.get("result").getAsDouble());
            assertEquals(0, res.body.get("a").getAsDouble());
        }
    }

    private static class Response {
        final int status;
        final java.util.Map<String, java.util.List<String>> headers;
        final JsonObject body;
        final long duration;

        Response(int status, java.util.Map<String, java.util.List<String>> headers, JsonObject body, long duration) {
            this.status = status;
            this.headers = headers;
            this.body = body;
            this.duration = duration;
        }
    }
}
