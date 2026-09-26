# ---------- build ----------
FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
# Copy the descriptor first so dependency download is cached separately from source changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=build /build/target/vierec-be.jar app.jar
# uploads/ is mounted as a volume (docker-compose); create it so the app user owns it.
RUN mkdir -p /app/uploads && chown -R app:app /app
USER app

ENV JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8383
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8383/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
