const http = require("http");

/**
 * Envoie une requête HTTP à une instance de serveur de test.
 * @param {http.Server} server - Instance déjà en écoute
 * @param {string} path        - Chemin + query string
 * @param {string} method      - Méthode HTTP (défaut : "GET")
 * @returns {Promise<{status, headers, body, duration}>}
 */
function request(server, path, method = "GET") {
    return new Promise((resolve, reject) => {
        const { port } = server.address();
        const start = Date.now();

        const req = http.request(
            {
                hostname: "127.0.0.1",
                port,
                path,
                method,
            },
            (res) => {
                let data = "";
                res.on("data", (chunk) => {
                    data += chunk;
                });
                res.on("end", () => {
                    const duration = Date.now() - start;
                    let body = null;
                    if (data) {
                        try {
                            body = JSON.parse(data);
                        } catch {
                            body = data;
                        }
                    }
                    resolve({
                        status: res.statusCode,
                        headers: res.headers,
                        body,
                        duration,
                    });
                });
            }
        );

        req.on("error", reject);
        req.end();
    });
}

module.exports = { request };
