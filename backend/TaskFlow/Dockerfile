FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /workspace/target/TaskFlow-*.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]