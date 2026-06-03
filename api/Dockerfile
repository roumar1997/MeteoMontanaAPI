# =========================================================
# Multi-stage Dockerfile para MeteoMontanaAPI
# Etapa 1: build con Maven (cache de dependencias)
# Etapa 2: runtime mínimo con JRE
# =========================================================

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Primero solo el pom.xml y el wrapper de Maven para aprovechar la caché:
# si pom.xml no cambia, Docker reusa la capa de "dependencias descargadas".
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

# Ahora el código fuente
COPY src src

# Build (skip tests — los corre CI antes de buildar la imagen)
RUN ./mvnw -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Usuario no-root por seguridad
RUN groupadd -r app && useradd -r -g app app

COPY --from=build /workspace/target/*.jar app.jar

USER app
EXPOSE 8080

# JVM flags ajustados para contenedores con memoria limitada
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
