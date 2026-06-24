<?php

use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestDox;
use PHPUnit\Framework\TestCase;
use Calculator\Calculator;

#[CoversClass(Calculator::class)]
class CalculatorTest extends TestCase
{
    private Calculator $calculator;

    protected function setUp(): void
    {
        $this->calculator = new Calculator();
    }

    #[Test]
    #[TestDox('add($a, $b) should return $expected')]
    #[DataProvider('addProvider')]
    public function testAdd(float $a, float $b, float $expected): void
    {
        $this->assertSame($expected, $this->calculator->add($a, $b));
    }

    public static function addProvider(): array
    {
        return [
            [2, 3, 5],
            [-5, -3, -8],
            [-5, 3, -2],
            [7, 0, 7],
        ];
    }

    #[Test]
    #[TestDox('add(0.1, 0.2) should return ≈0.3')]
    public function testAddFloat(): void
    {
        $this->assertEqualsWithDelta(0.3, $this->calculator->add(0.1, 0.2), 1e-15);
    }

    #[Test]
    #[TestDox('subtract($a, $b) should return $expected')]
    #[DataProvider('subtractProvider')]
    public function testSubtract(float $a, float $b, float $expected): void
    {
        $this->assertSame($expected, $this->calculator->subtract($a, $b));
    }

    public static function subtractProvider(): array
    {
        return [
            [10, 4, 6],
            [3, 10, -7],
            [5, 0, 5],
            [-5, -3, -2],
        ];
    }

    #[Test]
    #[TestDox('subtract(0.3, 0.1) should return ≈0.2')]
    public function testSubtractFloat(): void
    {
        $this->assertEqualsWithDelta(0.2, $this->calculator->subtract(0.3, 0.1), 1e-15);
    }

    #[Test]
    #[TestDox('multiply($a, $b) should return $expected')]
    #[DataProvider('multiplyProvider')]
    public function testMultiply(float $a, float $b, float $expected): void
    {
        $this->assertSame($expected, $this->calculator->multiply($a, $b));
    }

    public static function multiplyProvider(): array
    {
        return [
            [6, 7, 42],
            [0, 999, 0],
            [-3, -4, 12],
            [3, -4, -12],
        ];
    }

    #[Test]
    #[TestDox('multiply(0.1, 0.2) should return ≈0.02')]
    public function testMultiplyFloat(): void
    {
        $this->assertEqualsWithDelta(0.02, $this->calculator->multiply(0.1, 0.2), 1e-15);
    }

    #[Test]
    #[TestDox('divide($a, $b) should return $expected')]
    #[DataProvider('divideProvider')]
    public function testDivide(float $a, float $b, float $expected): void
    {
        $this->assertSame($expected, $this->calculator->divide($a, $b));
    }

    public static function divideProvider(): array
    {
        return [
            [20, 5, 4],
            [0, 5, 0],
            [-10, -2, 5],
            [-7, 2, -3.5],
        ];
    }

    #[Test]
    #[TestDox('divide(10, 3) should return ≈3.333')]
    public function testDivideFloat(): void
    {
        $this->assertEqualsWithDelta(3.333, $this->calculator->divide(10, 3), 1e-3);
    }

    #[Test]
    #[TestDox('divide by zero should throw')]
    public function testDivideByZero(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Division par zéro impossible.');
        $this->calculator->divide(10, 0);
    }
}
