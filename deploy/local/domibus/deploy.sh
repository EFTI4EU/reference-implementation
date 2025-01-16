#!/bin/sh
set -e
cd $(dirname $0)

echo "Starting up docker compose"
docker compose up -d

$SHELL
