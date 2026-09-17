# --- Étape 1 : construction ---------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Les dépendances sont résolues avant la copie des sources : le cache Docker
# n'est invalidé que lorsque le pom change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Étape 2 : exécution ------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# L'application tourne sans privilège et n'écrit que dans /app/media.
RUN addgroup -S musicstore && adduser -S musicstore -G musicstore \
    && mkdir -p /app/media && chown -R musicstore:musicstore /app

COPY --from=build --chown=musicstore:musicstore /build/target/*.jar /app/app.jar

USER musicstore
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    MUSICSTORE_STORAGE_LOCATION=/app/media

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
