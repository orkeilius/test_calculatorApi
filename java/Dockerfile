FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ ./src/
RUN mvn package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/calculator-api-1.0.0.jar /app/calculator-api.jar
EXPOSE 3000
CMD ["java", "-jar", "/app/calculator-api.jar"]
