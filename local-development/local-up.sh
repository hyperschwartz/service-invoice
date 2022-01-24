#!/usr/bin/env bash

./db-init.sh invoice-db
docker-compose up -d postgres
