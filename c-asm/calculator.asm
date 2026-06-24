; pas faire attention uwu

section .data
    welcome db 0dh, 0ah, "Ma super calculatrice", 0dh, 0ah 
    welcome_length equ $ - welcome

    choix db "Choisir le type d'operation: ", 0dh, 0ah
    choice_length equ $ - choix

    operator db "1. Addition", 0dh, 0ah

    operator_length equ $ - operator

    tmp db 0, 0 ; crée la variable tmp
    first_temp: db 0,0
    second_temp: db 0,0
    first db "Premier nombre: ", 0dh, 0ah
    first_length equ $ - first

    second db "Deuxieme nombre: ", 0dh, 0ah
    second_length equ $ - second

    response db "Resultat: "
    reponse_length equ $ - response

    plus db " + "
    plus_length equ $ - plus

    equal db " = "
    equal_length equ $ - equal

    result_char db 0

    dizaine db 0
    unitaire db 0

    retour_ligne db 0dh, 0ah
    retour_ligne_length equ $ - retour_ligne


section .text
global _start

_start:
    call welcome_message
    call choix_message
    call operator_message
    call get_input

    mov rax, 60
    xor rdi, rdi
    syscall

welcome_message:
    mov rax, 1 ; autorise l'ecriture 1 = ecriture, 0 = ntm
    mov rdi, 1 ; 
    mov rsi, welcome  ; genere le ptit message de "ma super calculatrice"
    mov rdx, welcome_length ; reserve les bits pour afficher le message
    syscall ; affiche le message
    ret ; retourne a la loop

choix_message:
    mov rax, 1
    mov rdi, 1
    mov rsi, choix
    mov rdx, choice_length
    syscall
    ret

operator_message:
    mov rax, 1
    mov rdi, 1
    mov rsi, operator
    mov rdx, operator_length
    syscall
    ret

get_input:
    mov rax, 0
    mov rdi, 0
    mov rsi, tmp
    mov rdx, 2
    syscall
    cmp byte [tmp], '1'
    je addition

    ret


addition:
    mov rax, 1
    mov rdi, 1
    mov rsi, first
    mov rdx, first_length
    syscall

    mov rax, 0
    mov rdi, 0
    mov rsi, first_temp
    mov rdx, 2
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, second
    mov rdx, second_length
    syscall

    mov rax, 0
    mov rdi, 0
    mov rsi, second_temp
    mov rdx, 2
    syscall

    mov r8, [first_temp]
    mov r9, [second_temp]

    sub r8, '0'
    sub r9, '0'

    mov r10, r8
    add r10, r9

    mov rax, 1
    mov rdi, 1
    mov rsi, response
    mov rdx, reponse_length
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, first_temp
    mov rdx, 1
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, plus
    mov rdx, plus_length
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, second_temp
    mov rdx, 1
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, equal
    mov rdx, equal_length
    syscall

    mov rax, r10
    xor rdx, rdx
    mov rcx, 10
    div rcx

    add al, '0'
    mov [dizaine], al

    add dl, '0'
    mov [unitaire], dl

    cmp byte [dizaine], '0'
    je afficher_unitaire

    mov rax, 1
    mov rdi, 1
    mov rsi, dizaine
    mov rdx, 1
    syscall

afficher_unitaire:
    mov rax, 1
    mov rdi, 1
    mov rsi, unitaire
    mov rdx, 1
    syscall

    mov rax, 1
    mov rdi, 1
    mov rsi, retour_ligne
    mov rdx, retour_ligne_length
    syscall
    ret