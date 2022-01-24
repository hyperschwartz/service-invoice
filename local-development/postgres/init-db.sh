#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="$POSTGRES_DB" <<-EOSQL
  drop database if exists invoice-db;
  create database invoice-db;
  \c invoice-db;
  grant all on database invoice-db to $POSTGRES_USER;
  create schema if not exists invoice authorization $POSTGRES_USER;
  grant all on schema invoice to $POSTGRES_USER;
EOSQL
