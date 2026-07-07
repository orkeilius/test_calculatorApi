#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <microhttpd.h>
#include <unistd.h>

extern double add_numbers(double a, double b);
extern double sub_numbers(double a, double b);
extern double mul_numbers(double a, double b);
extern double div_numbers(double a, double b);

#define PORT 3001
#define BODY_SIZE 256

typedef struct
{
    const char *name;
    double (*fn)(double, double);
} Operation;

static const Operation operations[] = {
    {"add", add_numbers},
    {"subtract", sub_numbers},
    {"multiply", mul_numbers},
    {"divide", div_numbers},
};

static const size_t operations_count = sizeof(operations) / sizeof(operations[0]);

static struct MHD_Response *make_json_response(const char *body)
{
    struct MHD_Response *resp = MHD_create_response_from_buffer(
        strlen(body), (void *)body, MHD_RESPMEM_MUST_COPY);

    MHD_add_response_header(resp, "Content-Type", "application/json");
    MHD_add_response_header(resp, "Access-Control-Allow-Origin", "*");
    return resp;
}

static enum MHD_Result send_response(struct MHD_Connection *conn,
                                     unsigned int status,
                                     const char *body)
{
    struct MHD_Response *resp = make_json_response(body);
    enum MHD_Result ret = MHD_queue_response(conn, status, resp);
    MHD_destroy_response(resp);
    return ret;
}

static enum MHD_Result handle_calculate(struct MHD_Connection *conn)
{
    const char *op = MHD_lookup_connection_value(conn, MHD_GET_ARGUMENT_KIND, "operation");
    const char *a_str = MHD_lookup_connection_value(conn, MHD_GET_ARGUMENT_KIND, "a");
    const char *b_str = MHD_lookup_connection_value(conn, MHD_GET_ARGUMENT_KIND, "b");

    if (!op || !a_str || !b_str)
        return send_response(conn, MHD_HTTP_BAD_REQUEST,
                             "{\"error\":\"parametres manquants\"}");

    double a = atof(a_str);
    double b = atof(b_str);

    for (size_t i = 0; i < operations_count; i++)
    {
        if (strcmp(op, operations[i].name) != 0)
            continue;

        if (strcmp(op, "divide") == 0 && b == 0.0)
            return send_response(conn, MHD_HTTP_BAD_REQUEST,
                                 "{\"error\":\"division par zero\"}");

        char body[BODY_SIZE];
        snprintf(body, sizeof(body), "{\"result\":%.2f}", operations[i].fn(a, b));
        return send_response(conn, MHD_HTTP_OK, body);
    }

    return send_response(conn, MHD_HTTP_BAD_REQUEST,
                         "{\"error\":\"operation inconnue\"}");
}

static enum MHD_Result handler(void *cls, struct MHD_Connection *conn,
                               const char *url, const char *method,
                               const char *version, const char *upload_data,
                               size_t *upload_data_size, void **con_cls)
{
    (void)cls;
    (void)version;
    (void)upload_data;
    (void)upload_data_size;
    (void)con_cls;

    if (strcmp(method, "GET") != 0)
        return send_response(conn, MHD_HTTP_METHOD_NOT_ALLOWED,
                             "{\"error\":\"methode non autorisee\"}");

    if (strcmp(url, "/calculate") == 0)
        return handle_calculate(conn);

    return send_response(conn, MHD_HTTP_NOT_FOUND,
                         "{\"error\":\"route inconnue\"}");
}

int main(void)
{
    struct MHD_Daemon *d = MHD_start_daemon(
        MHD_USE_THREAD_PER_CONNECTION, PORT,
        NULL, NULL, &handler, NULL, MHD_OPTION_END);

    if (!d)
    {
        fprintf(stderr, "Erreur: impossible de demarrer le serveur\n");
        return 1;
    }

    printf("API calculatrice sur http://0.0.0.0:%d\n", PORT);
    while (1)
        pause();
}