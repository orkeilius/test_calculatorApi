<?php

use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestDox;
use PHPUnit\Framework\TestCase;

class CalculatorApiTest extends TestCase
{
    private static ?int $pid = null;
    private static int $port;
    private const BASE_PATH = '/calculate';

    public static function setUpBeforeClass(): void
    {
        $sock = stream_socket_server('tcp://127.0.0.1:0', $errno, $errstr);
        if (!$sock) {
            throw new \RuntimeException("Could not find free port: $errstr");
        }
        $name = stream_socket_get_name($sock, false);
        self::$port = (int)substr($name, strrpos($name, ':') + 1);
        fclose($sock);

        $docRoot = escapeshellarg(__DIR__ . '/../public');
        $router = escapeshellarg(__DIR__ . '/../public/index.php');
        $cmd = sprintf(
            'php -S 127.0.0.1:%d -t %s %s > /dev/null 2>&1 & echo $!',
            self::$port,
            $docRoot,
            $router
        );
        exec($cmd, $output);
        self::$pid = (int)($output[0] ?? 0);
        usleep(300_000);
    }

    public static function tearDownAfterClass(): void
    {
        if (self::$pid) {
            exec('kill ' . self::$pid . ' 2>/dev/null');
        }
    }

    private static function request(string $path, string $method = 'GET'): array
    {
        $url = 'http://127.0.0.1:' . self::$port . $path;
        $start = hrtime(true);

        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER => true,
            CURLOPT_NOBODY => false,
            CURLOPT_CONNECTTIMEOUT => 2,
            CURLOPT_TIMEOUT => 5,
        ]);
        $response = curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $headerSize = (int)curl_getinfo($ch, CURLINFO_HEADER_SIZE);
        curl_close($ch);

        $duration = (hrtime(true) - $start) / 1_000_000;

        $headerStr = substr($response, 0, $headerSize);
        $bodyStr = substr($response, $headerSize);

        $headers = [];
        foreach (explode("\r\n", $headerStr) as $line) {
            if (str_contains($line, ': ')) {
                [$key, $value] = explode(': ', $line, 2);
                $headers[strtolower($key)] = $value;
            }
        }

        $body = $bodyStr !== '' ? json_decode($bodyStr, true) : null;

        return ['status' => $status, 'headers' => $headers, 'body' => $body, 'duration' => $duration];
    }

    #[Test]
    #[TestDox('valid request responds in less than 100ms')]
    public function testPerformanceValid(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=1&b=2');
        $this->assertLessThan(100, $res['duration']);
    }

    #[Test]
    #[TestDox('400 error responds in less than 100ms')]
    public function testPerformanceError(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=2');
        $this->assertLessThan(100, $res['duration']);
    }

    #[Test]
    #[TestDox('200 response has correct headers')]
    public function testHeaders200(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=1&b=2');
        $this->assertEquals('application/json; charset=utf-8', $res['headers']['content-type']);
        $this->assertEquals('*', $res['headers']['access-control-allow-origin']);
    }

    #[Test]
    #[TestDox('400 response has correct headers')]
    public function testHeaders400(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=2');
        $this->assertEquals('application/json; charset=utf-8', $res['headers']['content-type']);
        $this->assertEquals('*', $res['headers']['access-control-allow-origin']);
    }

    #[Test]
    #[TestDox('404 response has correct headers')]
    public function testHeaders404(): void
    {
        $res = self::request('/unknown');
        $this->assertEquals('application/json; charset=utf-8', $res['headers']['content-type']);
        $this->assertEquals('*', $res['headers']['access-control-allow-origin']);
    }

    #[Test]
    #[TestDox('OPTIONS returns 204 without body')]
    public function testOptions204(): void
    {
        $res = self::request(self::BASE_PATH, 'OPTIONS');
        $this->assertEquals(204, $res['status']);
        $this->assertNull($res['body']);
    }

    #[Test]
    #[TestDox('OPTIONS has Access-Control-Allow-Origin *')]
    public function testOptionsOrigin(): void
    {
        $res = self::request(self::BASE_PATH, 'OPTIONS');
        $this->assertEquals('*', $res['headers']['access-control-allow-origin']);
    }

    #[Test]
    #[TestDox('OPTIONS has Access-Control-Allow-Methods containing GET')]
    public function testOptionsMethods(): void
    {
        $res = self::request(self::BASE_PATH, 'OPTIONS');
        $this->assertStringContainsString('GET', $res['headers']['access-control-allow-methods']);
    }

    #[Test]
    #[TestDox('operation(a, b) returns expected result')]
    #[DataProvider('nominalProvider')]
    public function testNominal(string $operation, float $a, float $b, float $expected): void
    {
        $res = self::request(self::BASE_PATH . "?operation={$operation}&a={$a}&b={$b}");
        $this->assertEquals(200, $res['status']);
        $this->assertEquals($operation, $res['body']['operation']);
        $this->assertEquals($a, $res['body']['a']);
        $this->assertEquals($b, $res['body']['b']);
        $this->assertEquals($expected, $res['body']['result']);
    }

    public static function nominalProvider(): array
    {
        return [
            ['add', 2, 3, 5],
            ['subtract', 10, 4, 6],
            ['multiply', 6, 7, 42],
            ['divide', 20, 5, 4],
            ['add', -5, -3, -8],
            ['subtract', -5, -3, -2],
            ['multiply', -3, -4, 12],
            ['divide', -10, -2, 5],
        ];
    }

    #[Test]
    #[TestDox('divide(10, 3) ≈ 3.333')]
    public function testDecimalDivision(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=divide&a=10&b=3');
        $this->assertEquals(200, $res['status']);
        $this->assertEqualsWithDelta(3.333, $res['body']['result'], 1e-3);
    }

    #[Test]
    #[TestDox('add(1.5, 2.5) === 4')]
    public function testDecimalQuery(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=1.5&b=2.5');
        $this->assertEquals(200, $res['status']);
        $this->assertEquals(4, $res['body']['result']);
    }

    #[Test]
    #[TestDox('JSON 200 contract: has operation, a, b, result; no error')]
    public function testJsonContract(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=multiply&a=3&b=4');
        $this->assertArrayHasKey('operation', $res['body']);
        $this->assertArrayHasKey('a', $res['body']);
        $this->assertArrayHasKey('b', $res['body']);
        $this->assertArrayHasKey('result', $res['body']);
        $this->assertArrayNotHasKey('error', $res['body']);
    }

    #[Test]
    #[TestDox('POST returns 405 with error')]
    public function testPost405(): void
    {
        $res = self::request(self::BASE_PATH, 'POST');
        $this->assertEquals(405, $res['status']);
        $this->assertArrayHasKey('error', $res['body']);
    }

    #[Test]
    #[TestDox('POST has Allow header containing GET')]
    public function testPostAllowHeader(): void
    {
        $res = self::request(self::BASE_PATH, 'POST');
        $this->assertStringContainsString('GET', $res['headers']['allow']);
    }

    #[Test]
    #[TestDox('PUT returns 405')]
    public function testPut405(): void
    {
        $res = self::request(self::BASE_PATH, 'PUT');
        $this->assertEquals(405, $res['status']);
    }

    #[Test]
    #[TestDox('missing b returns 400 with Paramètres attendus')]
    public function testMissingB(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=2');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('Paramètres attendus', $res['body']['error']);
    }

    #[Test]
    #[TestDox('missing a returns 400 with Paramètres attendus')]
    public function testMissingA(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&b=2');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('Paramètres attendus', $res['body']['error']);
    }

    #[Test]
    #[TestDox('non-numeric a returns 400 with doivent être des nombres')]
    public function testNonNumericA(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=abc&b=3');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('doivent être des nombres', $res['body']['error']);
    }

    #[Test]
    #[TestDox('non-numeric b returns 400 with doivent être des nombres')]
    public function testNonNumericB(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=3&b=abc');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('doivent être des nombres', $res['body']['error']);
    }

    #[Test]
    #[TestDox('division by zero returns 400 with exact message')]
    public function testDivisionByZero(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=divide&a=10&b=0');
        $this->assertEquals(400, $res['status']);
        $this->assertEquals('Division par zéro impossible.', $res['body']['error']);
    }

    #[Test]
    #[TestDox('unknown operation returns 400 with Opération inconnue')]
    public function testUnknownOperation(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=modulo&a=10&b=3');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('Opération inconnue', $res['body']['error']);
    }

    #[Test]
    #[TestDox('missing operation returns 400 with Paramètres attendus')]
    public function testMissingOperation(): void
    {
        $res = self::request(self::BASE_PATH . '?a=5&b=3');
        $this->assertEquals(400, $res['status']);
        $this->assertStringContainsString('Paramètres attendus', $res['body']['error']);
    }

    #[Test]
    #[TestDox('JSON error contract: body has error, no result')]
    public function testErrorContract(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=2');
        $this->assertArrayHasKey('error', $res['body']);
        $this->assertArrayNotHasKey('result', $res['body']);
    }

    #[Test]
    #[TestDox('/unknown returns 404 with Route introuvable')]
    public function testUnknownRoute(): void
    {
        $res = self::request('/unknown');
        $this->assertEquals(404, $res['status']);
        $this->assertEquals('Route introuvable.', $res['body']['error']);
    }

    #[Test]
    #[TestDox('/ returns 404 with error')]
    public function testRoot(): void
    {
        $res = self::request('/');
        $this->assertEquals(404, $res['status']);
        $this->assertArrayHasKey('error', $res['body']);
    }

    #[Test]
    #[TestDox('/calculate/ returns 404 with error')]
    public function testTrailingSlash(): void
    {
        $res = self::request('/calculate/');
        $this->assertEquals(404, $res['status']);
        $this->assertArrayHasKey('error', $res['body']);
    }

    #[Test]
    #[TestDox('add(1e308, 1e308) returns null (Infinity)')]
    public function testLargeValue(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=1e308&b=1e308');
        $this->assertEquals(200, $res['status']);
        $this->assertNull($res['body']['result']);
    }

    #[Test]
    #[TestDox('add(-0, 5) returns result 5, a 0')]
    public function testNegativeZero(): void
    {
        $res = self::request(self::BASE_PATH . '?operation=add&a=-0&b=5');
        $this->assertEquals(200, $res['status']);
        $this->assertEquals(5, $res['body']['result']);
        $this->assertEquals(0, $res['body']['a']);
    }
}
