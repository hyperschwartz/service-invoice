#!/usr/bin/env bash

./db-init.sh provenance-invoice
docker-compose up -d postgres
