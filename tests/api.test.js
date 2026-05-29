const http = require("http");
const { requestHandler } = require("../src/server");
const { request } = require("./helpers/http");

describe("API /calculate", () => {
    let server;

    beforeAll((done) => {
        server = http.createServer(requestHandler);
        server.listen(0, "127.0.0.1", done); // port aléatoire
    });

    afterAll((done) => {
        server.close(done);
    });

    describe("Performance", () => {
        it("Une requête valide répond en moins de 100 ms", async () => {
            const { duration } = await request(
                server,
                "/calculate?operation=add&a=1&b=2"
            );
            expect(duration).toBeLessThan(100);
        });

        it("Une requête en erreur 400 répond en moins de 100 ms", async () => {
            const { duration } = await request(
                server,
                "/calculate?operation=add&a=2"
            );
            expect(duration).toBeLessThan(100);
        });
    });

    describe("Headers de réponse", () => {
        it("devrait avoir les bons headers sur une réponse 200", async () => {
            const { headers } = await request(
                server,
                "/calculate?operation=add&a=1&b=2"
            );
            expect(headers["content-type"]).toBe(
                "application/json; charset=utf-8"
            );
            expect(headers["access-control-allow-origin"]).toBe("*");
        });

        it("devrait avoir les bons headers sur une réponse 400", async () => {
            const { headers } = await request(
                server,
                "/calculate?operation=add&a=2"
            );
            expect(headers["content-type"]).toBe(
                "application/json; charset=utf-8"
            );
            expect(headers["access-control-allow-origin"]).toBe("*");
        });

        it("devrait avoir les bons headers sur une réponse 404", async () => {
            const { headers } = await request(server, "/unknown");
            expect(headers["content-type"]).toBe(
                "application/json; charset=utf-8"
            );
            expect(headers["access-control-allow-origin"]).toBe("*");
        });
    });

    describe("OPTIONS /calculate — preflight CORS", () => {
        it("devrait retourner 204 sans body", async () => {
            const { status, body } = await request(server, "/calculate", "OPTIONS");
            expect(status).toBe(204);
            expect(body).toBeNull();
        });

        it("devrait avoir Access-Control-Allow-Origin *", async () => {
            const { headers } = await request(server, "/calculate", "OPTIONS");
            expect(headers["access-control-allow-origin"]).toBe("*");
        });

        it("devrait avoir Access-Control-Allow-Methods contenant GET", async () => {
            const { headers } = await request(server, "/calculate", "OPTIONS");
            expect(headers["access-control-allow-methods"]).toContain("GET");
        });
    });

    describe("GET /calculate — cas nominaux", () => {
        it.each`
      operation      | a      | b      | expected
      ${"add"}       | ${2}   | ${3}   | ${5}
      ${"subtract"}  | ${10}  | ${4}   | ${6}
      ${"multiply"}  | ${6}   | ${7}   | ${42}
      ${"divide"}    | ${20}  | ${5}   | ${4}
      ${"add"}       | ${-5}  | ${-3}  | ${-8}
      ${"subtract"}  | ${-5}  | ${-3}  | ${-2}
      ${"multiply"}  | ${-3}  | ${-4}  | ${12}
      ${"divide"}    | ${-10} | ${-2}  | ${5}
    `(
            "$operation($a, $b) devrait retourner $expected",
            async ({ operation, a, b, expected }) => {
                const { status, body } = await request(
                    server,
                    `/calculate?operation=${operation}&a=${a}&b=${b}`
                );
                expect(status).toBe(200);
                expect(body).toMatchObject({ operation, a, b, result: expected });
            }
        );

        it("Division décimale : divide(10, 3) ≈ 3.333", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=divide&a=10&b=3"
            );
            expect(status).toBe(200);
            expect(body.result).toBeCloseTo(3.333);
        });

        it("Décimaux en query string : add(1.5, 2.5) === 4", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=1.5&b=2.5"
            );
            expect(status).toBe(200);
            expect(body.result).toBe(4);
        });

        it("Contrat JSON 200 : body contient operation, a, b, result et pas error", async () => {
            const { body } = await request(
                server,
                "/calculate?operation=multiply&a=3&b=4"
            );
            expect(body).toHaveProperty("operation");
            expect(body).toHaveProperty("a");
            expect(body).toHaveProperty("b");
            expect(body).toHaveProperty("result");
            expect(body).not.toHaveProperty("error");
        });
    });

    describe("Méthode non autorisée", () => {
        it("POST devrait retourner 405 avec error", async () => {
            const { status, body } = await request(server, "/calculate", "POST");
            expect(status).toBe(405);
            expect(body).toHaveProperty("error");
        });

        it("POST devrait avoir le header allow contenant GET", async () => {
            const { headers } = await request(server, "/calculate", "POST");
            expect(headers["allow"]).toContain("GET");
        });

        it("PUT devrait retourner 405", async () => {
            const { status } = await request(server, "/calculate", "PUT");
            expect(status).toBe(405);
        });
    });

    describe("GET /calculate — erreurs 400", () => {
        it("b manquant : status 400, message /Paramètres attendus/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=2"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/Paramètres attendus/);
        });

        it("a manquant : status 400, message /Paramètres attendus/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&b=2"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/Paramètres attendus/);
        });

        it("a non numérique : status 400, message /doivent être des nombres/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=abc&b=3"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/doivent être des nombres/);
        });

        it("b non numérique : status 400, message /doivent être des nombres/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=3&b=abc"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/doivent être des nombres/);
        });

        it('Division par zéro : status 400, message exact "Division par zéro impossible."', async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=divide&a=10&b=0"
            );
            expect(status).toBe(400);
            expect(body.error).toBe("Division par zéro impossible.");
        });

        it("Opération inconnue : status 400, message /Opération inconnue/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=modulo&a=10&b=3"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/Opération inconnue/);
        });

        it("operation absent : status 400, message /Paramètres attendus/", async () => {
            const { status, body } = await request(
                server,
                "/calculate?a=5&b=3"
            );
            expect(status).toBe(400);
            expect(body.error).toMatch(/Paramètres attendus/);
        });

        it("Contrat JSON erreur : body a error et pas result", async () => {
            const { body } = await request(
                server,
                "/calculate?operation=add&a=2"
            );
            expect(body).toHaveProperty("error");
            expect(body).not.toHaveProperty("result");
        });
    });

    describe("GET — autres routes", () => {
        it('Route inconnue /unknown : status 404, body.error === "Route introuvable."', async () => {
            const { status, body } = await request(server, "/unknown");
            expect(status).toBe(404);
            expect(body.error).toBe("Route introuvable.");
        });

        it("Racine / : status 404, body a error", async () => {
            const { status, body } = await request(server, "/");
            expect(status).toBe(404);
            expect(body).toHaveProperty("error");
        });

        it("Slash final /calculate/ : status 404, body a error", async () => {
            const { status, body } = await request(server, "/calculate/");
            expect(status).toBe(404);
            expect(body).toHaveProperty("error");
        });
    });

    describe("Cas limites — edge cases", () => {
        it("Très grande valeur : add(1e308, 1e308) retourne Infinity ou null", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=1e308&b=1e308"
            );
            expect(status).toBe(200);
            expect(
                body.result === null || body.result === "Infinity" || body.result === Infinity
            ).toBe(true);
        });

        it("a=-0 : add(-0, 5) retourne result === 5, a === 0", async () => {
            const { status, body } = await request(
                server,
                "/calculate?operation=add&a=-0&b=5"
            );
            expect(status).toBe(200);
            expect(body.result).toBe(5);
            expect(body.a).toBe(0);
        });
    });
});
