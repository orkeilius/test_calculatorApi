#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>

extern double add_numbers(double a, double b);
extern double sub_numbers(double a, double b);
extern double mul_numbers(double a, double b);
extern double div_numbers(double a, double b);

static void send_json(int client_fd, int status, const char *status_text, const char *body)
{
    dprintf(
        client_fd,
        "HTTP/1.1 %d %s\r\n"
        "Content-Type: application/json\r\n"
        "Access-Control-Allow-Origin: *\r\n"
        "Content-Length: %zu\r\n"
        "\r\n"
        "%s",
        status,
        status_text,
        strlen(body),
        body);
}

int main(void)
{
    int server_fd, client_fd;
    struct sockaddr_in addr;
    char buffer[4096];
    char path[1024];

    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0)
    {
        perror("socket");
        return 1;
    }

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = inet_addr("127.0.0.1");
    addr.sin_port = htons(3001);

    if (bind(server_fd, (struct sockaddr *)&addr, sizeof(addr)) < 0)
    {
        perror("bind");
        close(server_fd);
        return 1;
    }

    if (listen(server_fd, 10) < 0)
    {
        perror("listen");
        close(server_fd);
        return 1;
    }

    printf("API de ma ptite calculatrice sur http://127.0.0.1:3001\n");

    while (1)
    {
        client_fd = accept(server_fd, NULL, NULL);
        if (client_fd < 0)
        {
            perror("accept");
            continue;
        }

        ssize_t n = read(client_fd, buffer, sizeof(buffer) - 1);
        if (n <= 0)
        {
            close(client_fd);
            continue;
        }

        buffer[n] = '\0';

        if (sscanf(buffer, "GET %1023s", path) != 1)
        {
            send_json(client_fd, 400, "Bad Request", "{\"error\":\"requete invalide\"}");
            close(client_fd);
            continue;
        }

        double a, b;
        char body[256];

        if (sscanf(path, "/api/add?a=%lf&b=%lf", &a, &b) == 2)
        {
            snprintf(body, sizeof(body), "{\"result\":%.2f}", add_numbers(a, b));
            send_json(client_fd, 200, "OK", body);
        }
        else if (sscanf(path, "/api/sub?a=%lf&b=%lf", &a, &b) == 2)
        {
            snprintf(body, sizeof(body), "{\"result\":%.2f}", sub_numbers(a, b));
            send_json(client_fd, 200, "OK", body);
        }
        else if (sscanf(path, "/api/mul?a=%lf&b=%lf", &a, &b) == 2)
        {
            snprintf(body, sizeof(body), "{\"result\":%.2f}", mul_numbers(a, b));
            send_json(client_fd, 200, "OK", body);
        }
        else if (sscanf(path, "/api/div?a=%lf&b=%lf", &a, &b) == 2)
        {
            if (b == 0.0)
            {
                send_json(client_fd, 400, "Bad Request", "{\"error\":\"division par zero\"}");
            }
            else
            {
                snprintf(body, sizeof(body), "{\"result\":%.2f}", div_numbers(a, b));
                send_json(client_fd, 200, "OK", body);
            }
        }
        else
        {
            send_json(client_fd, 404, "Not Found", "{\"error\":\"route inconnue\"}");
        }

        close(client_fd);
    }

    close(server_fd);
    return 0;
}