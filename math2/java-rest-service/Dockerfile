FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY java-api/pom.xml java-api/pom.xml
COPY java-rest-service/pom.xml java-rest-service/pom.xml
COPY java-api/src java-api/src
COPY java-rest-service/src java-rest-service/src
RUN mvn -B -pl java-rest-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system math2 \
    && useradd --system --gid math2 --home-dir /app math2
WORKDIR /app
COPY --from=build /workspace/java-rest-service/target/math2-rest-service-*.jar app.jar
RUN mkdir /app/data && chown -R math2:math2 /app
USER math2
ENV MATH2_SERVER_ADDRESS=0.0.0.0
ENV MATH2_DATASOURCE_URL=jdbc:h2:file:/app/data/cache
EXPOSE 8080
VOLUME ["/app/data"]
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD ["curl", "--fail", "--silent", "http://127.0.0.1:8080/actuator/health"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
