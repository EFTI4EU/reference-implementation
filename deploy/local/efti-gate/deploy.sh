#!/usr/bin/env bash

# Stop on fail
set -e
cd $(dirname $0)

ARG=${1:-}

if [[ "$ARG" == "skip-tests" ]]; then
  SKIP_TESTS="-DskipTests"
elif [[ "$ARG" == "" ]]; then
  # no-op
  SKIP_TESTS=""
else
  echo "Unsupported parameter $1"
  exit 1
fi

projectPomFile=../../../implementation/pom.xml

echo "Cleaning up..."
mvn -B clean --file $projectPomFile

echo "Building..."
mvn -B package --file $projectPomFile $SKIP_TESTS

echo "Copying apps..."
cp -rf ../../../implementation/gate/target/gate-*.jar ./gate/efti-gate.jar
cp -rf ../../../implementation/platform-gate-simulator/target/platform-gate-simulator-*.jar ./platform/platform-simulator.jar

echo "Starting up docker compose"
docker compose up -d
docker compose restart efti-gate-BO efti-gate-LI efti-gate-SY

wait_for_gate() {
    gate_port=$1
    echo "Waiting for the gate to be up and running at $gate_port..."
    until $(curl --output /dev/null --silent --head --fail http://localhost:$gate_port/actuator/health); do
        echo "Waiting for the gate to be up and running at $gate_port..."
        sleep 5
    done
}

wait_for_gate 8880
wait_for_gate 8881
wait_for_gate 8882

echo "Gates are up and running"

echo "Configure the gates with initial data"
# This goes through the schemas in the common database and configures all of them with the same data
for schema in eftibo eftili eftisy; do
  echo 'Configuring gate with initial data for database: ' $schema
  sed "1iset search_path to $schema;" ./gate-db/gate-config.sql | docker exec -i reference-gate-shared-db psql -U efti -d efti
done
