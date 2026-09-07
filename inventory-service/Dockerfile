# Multi-stage build: compile from source, then extract Spring Boot's
# layered jar so the image's layers separate rarely-changing dependencies
# from the actual application code - only the last layer changes on a
# typical rebuild.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

EXPOSE 8081
# MaxRAMPercentage, not a fixed -Xmx: the container's actual memory limit
# is set by docker-compose/Kubernetes, not baked into the image.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
