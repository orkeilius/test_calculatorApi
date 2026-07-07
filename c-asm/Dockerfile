FROM ubuntu:24.04

RUN apt-get update && apt-get install -y \
    gcc \
    nasm \
    libmicrohttpd-dev \
    make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

RUN nasm -f elf64 calculator.asm -o calculator.o
RUN gcc server.c calculator.o -o calculator-api -lmicrohttpd

EXPOSE 3001

CMD ["./calculator-api"]