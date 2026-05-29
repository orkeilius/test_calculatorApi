const http = require("http");
const url = require("url");
const Calculator = require("./calculator");

const PORT = 3000;
const calculator = new Calculator();

function requestHandler(req, res) {
    // 1. Set CORS headers on all responses
    res.setHeader("Content-Type", "application/json; charset=utf-8");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

    // 2. OPTIONS → 204 empty
    if (req.method === "OPTIONS") {
        res.writeHead(204);
        res.end();
        return;
    }

    // 3. Method ≠ GET → 405
    if (req.method !== "GET") {
        res.setHeader("Allow", "GET, OPTIONS");
        res.writeHead(405);
        res.end(JSON.stringify({ error: "Méthode non autorisée. Utiliser GET." }));
        return;
    }

    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    const query = parsedUrl.query;

    // 4. Route ≠ /calculate → 404
    if (pathname !== "/calculate") {
        res.writeHead(404);
        res.end(JSON.stringify({ error: "Route introuvable." }));
        return;
    }

    const { operation, a, b } = query;

    // 5. Missing parameters
    if (
        operation === undefined ||
        a === undefined ||
        b === undefined
    ) {
        res.writeHead(400);
        res.end(
            JSON.stringify({ error: "Paramètres attendus : operation, a, b" })
        );
        return;
    }

    // 6. Convert a and b to Number; if NaN → 400
    const numA = Number(a);
    const numB = Number(b);
    if (isNaN(numA) || isNaN(numB)) {
        res.writeHead(400);
        res.end(
            JSON.stringify({
                error: "Les paramètres a et b doivent être des nombres.",
            })
        );
        return;
    }

    // 7-8. Execute operation via Calculator
    let result;
    try {
        switch (operation) {
            case "add":
                result = calculator.add(numA, numB);
                break;
            case "subtract":
                result = calculator.subtract(numA, numB);
                break;
            case "multiply":
                result = calculator.multiply(numA, numB);
                break;
            case "divide":
                result = calculator.divide(numA, numB);
                break;
            default:
                res.writeHead(400);
                res.end(
                    JSON.stringify({
                        error:
                            "Opération inconnue. Utiliser : add, subtract, multiply, divide",
                    })
                );
                return;
        }
    } catch (err) {
        // Division by zero caught here
        res.writeHead(400);
        res.end(JSON.stringify({ error: err.message }));
        return;
    }

    // 9. Success → 200
    res.writeHead(200);
    res.end(
        JSON.stringify({
            operation,
            a: numA,
            b: numB,
            result,
        })
    );
}

const server = http.createServer(requestHandler);

module.exports = { requestHandler, server };

if (require.main === module) {
    server.listen(PORT, () => {
        console.log(`Serveur démarré sur http://localhost:${PORT}`);
    });
}
