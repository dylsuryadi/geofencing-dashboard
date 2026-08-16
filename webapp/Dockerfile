FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests -o

FROM eclipse-temurin:25-jre
WORKDIR /deployments
COPY --from=build /build/target/quarkus-app/lib ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/
EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
