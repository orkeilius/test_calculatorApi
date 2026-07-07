#!/bin/sh
set -e

echo "Assemblage de calculator.asm..."
nasm -f elf64 calculator.asm -o calculator.o

echo "Compilation de test_calculator.c..."
gcc -IUnity/src \
    -DUNITY_INCLUDE_DOUBLE \
    -DUNITY_DOUBLE_PRECISION=1e-12 \
    -c test_calculator.c -o test_calculator.o

echo "Link édition de liens..."
gcc -IUnity/src \
    -DUNITY_INCLUDE_DOUBLE \
    -DUNITY_DOUBLE_PRECISION=1e-12 \
    test_calculator.o calculator.o Unity/src/unity.c \
    -o test_calc -lm

echo "Exécution des tests..."
./test_calc