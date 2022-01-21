#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="$POSTGRES_DB" <<-EOSQL
  drop database if exists provenance-invoice;
  create database provenance-invoice;
  \c provenance-invoice;
  grant all on database provenance-invoice to $POSTGRES_USER;
  create schema if not exists invoice authorization $POSTGRES_USER;
  grant all on schema invoice to $POSTGRES_USER;
EOSQL
