#include <stdio.h>
#include <limits.h>
#include <math.h>

extern double add_numbers(double a, double b);
extern double sub_numbers(double a, double b);
extern double mul_numbers(double a, double b);
extern double div_numbers(double a, double b);

static int tests_run = 0;
static int tests_failed = 0;

#define CHECK_EQ(name, actual, expected)                                              \
    do                                                                                \
    {                                                                                 \
        double _actual = (actual);                                                    \
        double _expected = (expected);                                                \
        tests_run++;                                                                  \
        if (fabs(_actual - _expected) > 1e-9)                                         \
        {                                                                             \
            tests_failed++;                                                           \
            printf("\033[31m%s | Valeur attendue=%.2f Valeur actuelle=%.2f\033[0m\n", \
                   name, _expected, _actual);                                         \
        }                                                                             \
        else                                                                          \
        {                                                                             \
            printf("\033[32m%s | Valeur attendue=%.2f Valeur actuelle=%.2f\033[0m\n", \
                   name, _expected, _actual);                                         \
        }                                                                             \
    } while (0)

int main(void)
{
    printf("Test addition\n");
    CHECK_EQ("add 5 + 8", add_numbers(5, 8), 13);
    CHECK_EQ("add 0 + 0", add_numbers(0, 0), 0);
    CHECK_EQ("add -5 + 2", add_numbers(-5, 2), -3);
    CHECK_EQ("add -8 + -4", add_numbers(-8, -4), -12);
    CHECK_EQ("add 999 + 1", add_numbers(999, 1), 1000);

    printf("\nTest soustraction \n");
    CHECK_EQ("sub 9 - 4", sub_numbers(9, 4), 5);
    CHECK_EQ("sub 3 - 7", sub_numbers(3, 7), -4);
    CHECK_EQ("sub 0 - 0", sub_numbers(0, 0), 0);
    CHECK_EQ("sub -5 - 2", sub_numbers(-5, 2), -7);
    CHECK_EQ("sub -5 - -2", sub_numbers(-5, -2), -3);

    printf("\nTest multiplication \n");
    CHECK_EQ("mul 6 * 7", mul_numbers(6, 7), 42);
    CHECK_EQ("mul 0 * 9", mul_numbers(0, 9), 0);
    CHECK_EQ("mul -3 * 4", mul_numbers(-3, 4), -12);
    CHECK_EQ("mul -3 * -4", mul_numbers(-3, -4), 12);
    CHECK_EQ("mul 1 * 999", mul_numbers(1, 999), 999);
    CHECK_EQ("mul 1.5 * 2", mul_numbers(1.5, 2), 3);

    printf("\nTest division \n");
    CHECK_EQ("div 20 / 5", div_numbers(20, 5), 4.0);
    CHECK_EQ("div 9 / 2", div_numbers(9, 2), 4.5);
    CHECK_EQ("div -9 / 2", div_numbers(-9, 2), -4.5);
    CHECK_EQ("div 9 / -2", div_numbers(9, -2), -4.5);
    CHECK_EQ("div -9 / -2", div_numbers(-9, -2), 4.5);
    CHECK_EQ("div 10 / 3", div_numbers(10.0, 3.0), 3.3333333333333335);

    printf("\nTest division par zéro\n");
    double r = div_numbers(0, 0);
    if (isnan(r))
    {
        tests_run++;
        printf("\033[32mdiv 0 / 0 | Valeur attendue=NaN Valeur actuelle=NaN\033[0m\n");
    }

    printf("\nTest qui doit cassé : \n");
    CHECK_EQ("div 20 / 5", div_numbers(20, 5), 7.0);
    CHECK_EQ("mul 6 * 7", mul_numbers(6, 7), 75);
    CHECK_EQ("sub 9 - 4", sub_numbers(9, 4), 1);
    CHECK_EQ("add 5 + 8", add_numbers(5, 8), 47);

    printf("\nRésultat des ptit tests\n");
    printf("Tests fait : %d\n", tests_run);
    printf("Tests cacaté  : %d\n", tests_failed);
    printf("Tests trop cool qui fonctionne  : %d\n", tests_run - tests_failed);

    return tests_failed == 0 ? 0 : 1;
}