FROM php:8.2-cli-alpine
WORKDIR /app
COPY src/ ./src/
COPY public/ ./public/
EXPOSE 3000
CMD ["php", "-S", "0.0.0.0:3000", "-t", "public/", "public/index.php"]
