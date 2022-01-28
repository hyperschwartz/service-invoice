#!/bin/bash

# Run with ./localnet.sh up. Spins up the local database that the application uses
function up {
    docker volume prune -f
    # Ensure the local postgres container is built to ensure that our magic self-initializing database comes up
    build-test-containers
    # Spin up the local dependencies
    docker-compose -f docker/local/app-dependencies.yml up --build -d
}

function build-test-containers {
    # First, eradicate running versions of the containers
    docker ps -a | awk '{ print $1,$2 }' | grep invoice-postgres:latest | awk '{print $1 }' | xargs -I {} docker rm -f {}
    # Pop on into our docker directory for building and executing
    pushd ./docker/local || exit
    # Build and tag the postgres container using the dockerfile
    docker build -t invoice-postgres -f Dockerfile-postgres .
    # Pop some additional tags on there for funsies
    docker tag invoice-postgres:latest invoice-postgres:current
    # Back from whence ye came!
    popd || exit
}

# Funnel input to a function
${1}
