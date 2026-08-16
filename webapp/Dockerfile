FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:25
WORKDIR /deployments
COPY --from=build /build/target/quarkus-app/lib ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/
EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
