# Multi-stage Docker build for Spendly
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /app/data
COPY --from=build /app/target/spendly-*.jar app.jar

EXPOSE 10000
EXPOSE 8080
ENV PORT=10000
ENV SPRING_PROFILES_ACTIVE=h2
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xss512k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${PORT:-10000} -jar app.jar"]
