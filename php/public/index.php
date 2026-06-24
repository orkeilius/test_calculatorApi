<?php

require_once __DIR__ . '/../src/Calculator.php';

use Calculator\Calculator;

function setCorsHeaders(): void
{
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization');
}

function sendJson(int $status, string $json): void
{
    http_response_code($status);
    echo $json;
}

function formatNumber(float $d): string
{
    if (is_nan($d) || is_infinite($d)) {
        return 'null';
    }
    if ($d === (float)(int)$d) {
        return (string)(int)$d;
    }
    return (string)$d;
}

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

setCorsHeaders();

if ($method === 'OPTIONS') {
    http_response_code(204);
    return;
}

if ($method !== 'GET') {
    header('Allow: GET, OPTIONS');
    sendJson(405, '{"error":"Méthode non autorisée. Utiliser GET."}');
    return;
}

if ($path !== '/calculate') {
    sendJson(404, '{"error":"Route introuvable."}');
    return;
}

$op = $_GET['operation'] ?? null;
$aStr = $_GET['a'] ?? null;
$bStr = $_GET['b'] ?? null;

if ($op === null || $aStr === null || $bStr === null) {
    sendJson(400, '{"error":"Paramètres attendus : operation, a, b"}');
    return;
}

if (!is_numeric($aStr) || !is_numeric($bStr)) {
    sendJson(400, '{"error":"Les paramètres a et b doivent être des nombres."}');
    return;
}

$a = (float)$aStr;
$b = (float)$bStr;

$calculator = new Calculator();

try {
    switch ($op) {
        case 'add':
            $result = $calculator->add($a, $b);
            break;
        case 'subtract':
            $result = $calculator->subtract($a, $b);
            break;
        case 'multiply':
            $result = $calculator->multiply($a, $b);
            break;
        case 'divide':
            $result = $calculator->divide($a, $b);
            break;
        default:
            sendJson(400, '{"error":"Opération inconnue. Utiliser : add, subtract, multiply, divide"}');
            return;
    }
} catch (\InvalidArgumentException $e) {
    sendJson(400, '{"error":"' . $e->getMessage() . '"}');
    return;
}

$json = sprintf(
    '{"operation":"%s","a":%s,"b":%s,"result":%s}',
    $op,
    formatNumber($a),
    formatNumber($b),
    formatNumber($result)
);
sendJson(200, $json);
