#!/usr/bin/env bash

mkdir -p scaffold-tmp

docker run --rm -v "$PWD/scaffold-tmp:/app" -w /app \
    maven:3.9-eclipse-temurin-25-noble \
    mvn io.quarkus.platform:quarkus-maven-plugin:3.38.2:create \
    -DplatformVersion=3.33.2 \
    -DprojectGroupId=dev.geofencing -DprojectArtifactId=webapp \
    -Dextensions="rest,rest-jackson,jdbc-postgresql" 2>&1 | tee scaffold.log

cp -a scaffold-tmp/webapp/. webapp/
#rm -rf scaffold-tmp
