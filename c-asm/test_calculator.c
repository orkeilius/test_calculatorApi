#include "unity.h"
#include <math.h>

extern double add_numbers(double a, double b);
extern double sub_numbers(double a, double b);
extern double mul_numbers(double a, double b);
extern double div_numbers(double a, double b);

void test_add_numbers(void)
{
    TEST_ASSERT_EQUAL_DOUBLE(13.0, add_numbers(5, 8));
    TEST_ASSERT_EQUAL_DOUBLE(0.0, add_numbers(0, 0));
    TEST_ASSERT_EQUAL_DOUBLE(-3.0, add_numbers(-5, 2));
    TEST_ASSERT_EQUAL_DOUBLE(-12.0, add_numbers(-8, -4));
    TEST_ASSERT_EQUAL_DOUBLE(1000.0, add_numbers(999, 1));
}

void test_sub_numbers(void)
{
    TEST_ASSERT_EQUAL_DOUBLE(5.0, sub_numbers(9, 4));
    TEST_ASSERT_EQUAL_DOUBLE(-4.0, sub_numbers(3, 7));
    TEST_ASSERT_EQUAL_DOUBLE(0.0, sub_numbers(0, 0));
    TEST_ASSERT_EQUAL_DOUBLE(-7.0, sub_numbers(-5, 2));
    TEST_ASSERT_EQUAL_DOUBLE(-3.0, sub_numbers(-5, -2));
}

void test_mul_numbers(void)
{
    TEST_ASSERT_EQUAL_DOUBLE(42.0, mul_numbers(6, 7));
    TEST_ASSERT_EQUAL_DOUBLE(0.0, mul_numbers(0, 9));
    TEST_ASSERT_EQUAL_DOUBLE(-12.0, mul_numbers(-3, 4));
    TEST_ASSERT_EQUAL_DOUBLE(12.0, mul_numbers(-3, -4));
    TEST_ASSERT_EQUAL_DOUBLE(999.0, mul_numbers(1, 999));
    TEST_ASSERT_DOUBLE_WITHIN(1e-9, 3.0, mul_numbers(1.5, 2));
}

void test_div_numbers(void)
{
    TEST_ASSERT_EQUAL_DOUBLE(4.0, div_numbers(20, 5));
    TEST_ASSERT_EQUAL_DOUBLE(4.5, div_numbers(9, 2));
    TEST_ASSERT_EQUAL_DOUBLE(-4.5, div_numbers(-9, 2));
    TEST_ASSERT_EQUAL_DOUBLE(-4.5, div_numbers(9, -2));
    TEST_ASSERT_EQUAL_DOUBLE(4.5, div_numbers(-9, -2));
    TEST_ASSERT_DOUBLE_WITHIN(1e-9, 3.3333333333333335, div_numbers(10.0, 3.0));
}

void test_div_zero_zero_returns_nan(void)
{
    double r = div_numbers(0, 0);
    TEST_ASSERT_TRUE(isnan(r));
}



int main(void)
{
    UNITY_BEGIN();

    RUN_TEST(test_add_numbers);
    RUN_TEST(test_sub_numbers);
    RUN_TEST(test_mul_numbers);
    RUN_TEST(test_div_numbers);
    RUN_TEST(test_div_zero_zero_returns_nan);

    return UNITY_END();
}