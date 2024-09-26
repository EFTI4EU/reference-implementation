#!/bin/sh

# Stop on fail
set -e

projectPomFile=../../../implementation/pom.xml

echo "Cleaning up..."
mvn -B clean --file $projectPomFile

echo "Building..."
mvn -B package --file $projectPomFile

echo "Copying apps..."
cp -rf ../../../implementation/gate/target/efti-gate-*.jar ./gate/efti-gate.jar
cp -rf ../../../implementation/platform-gate-simulator/target/platform-gate-simulator-*.jar ./platform/platform-simulator.jar

echo "Starting up docker compose"
docker compose up

$SHELL
