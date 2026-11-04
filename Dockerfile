# Stage 1: Build da aplicação com Maven e Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependências Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Compilar e empacotar a aplicação
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Imagem final enxuta para produção
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criar usuário sem privilégios de root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

VOLUME /tmp
COPY --from=build --chown=appuser:appgroup /app/target/gerenciador-financeiro-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
