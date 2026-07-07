section .text
global add_numbers
global sub_numbers
global mul_numbers
global div_numbers

add_numbers:
    addsd xmm0, xmm1
    ret

sub_numbers:
    subsd xmm0, xmm1
    ret

mul_numbers:
    mulsd xmm0, xmm1    
    ret

div_numbers:
    divsd xmm0, xmm1
    ret

section .note.GNU-stack noalloc noexec nowrite progbits