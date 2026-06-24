package calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Calculator")
class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Calculator();
    }

    @Nested
    @DisplayName("add")
    class Add {

        @ParameterizedTest
        @CsvSource({
                "2, 3, 5",
                "-5, -3, -8",
                "-5, 3, -2",
                "7, 0, 7"
        })
        @DisplayName("should return a + b")
        void testAdd(double a, double b, double expected) {
            assertEquals(expected, calculator.add(a, b));
        }

        @Test
        @DisplayName("should return ≈0.3 for add(0.1, 0.2)")
        void testAddFloat() {
            assertEquals(0.3, calculator.add(0.1, 0.2), 1e-15);
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @ParameterizedTest
        @CsvSource({
                "10, 4, 6",
                "3, 10, -7",
                "5, 0, 5",
                "-5, -3, -2"
        })
        @DisplayName("should return a - b")
        void testSubtract(double a, double b, double expected) {
            assertEquals(expected, calculator.subtract(a, b));
        }

        @Test
        @DisplayName("should return ≈0.2 for subtract(0.3, 0.1)")
        void testSubtractFloat() {
            assertEquals(0.2, calculator.subtract(0.3, 0.1), 1e-15);
        }
    }

    @Nested
    @DisplayName("multiply")
    class Multiply {

        @ParameterizedTest
        @CsvSource({
                "6, 7, 42",
                "0, 999, 0",
                "-3, -4, 12",
                "3, -4, -12"
        })
        @DisplayName("should return a * b")
        void testMultiply(double a, double b, double expected) {
            assertEquals(expected, calculator.multiply(a, b));
        }

        @Test
        @DisplayName("should return ≈0.02 for multiply(0.1, 0.2)")
        void testMultiplyFloat() {
            assertEquals(0.02, calculator.multiply(0.1, 0.2), 1e-15);
        }
    }

    @Nested
    @DisplayName("divide")
    class Divide {

        @ParameterizedTest
        @CsvSource({
                "20, 5, 4",
                "0, 5, 0",
                "-10, -2, 5",
                "-7, 2, -3.5"
        })
        @DisplayName("should return a / b")
        void testDivide(double a, double b, double expected) {
            assertEquals(expected, calculator.divide(a, b));
        }

        @Test
        @DisplayName("should return ≈3.333 for divide(10, 3)")
        void testDivideFloat() {
            assertEquals(3.333, calculator.divide(10, 3), 1e-3);
        }

        @Test
        @DisplayName("should throw for division by zero")
        void testDivideByZero() {
            ArithmeticException ex = assertThrows(ArithmeticException.class,
                    () -> calculator.divide(10, 0));
            assertEquals("Division par zéro impossible.", ex.getMessage());
        }
    }
}
