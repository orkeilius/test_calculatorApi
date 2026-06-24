FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm i

FROM node:20-alpine
WORKDIR /app
COPY --from=build /app/node_modules ./node_modules
COPY src/ ./src/
EXPOSE 3000
CMD ["node", "src/server.js"]
