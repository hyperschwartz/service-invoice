#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname="$POSTGRES_DB" <<-EOSQL
  drop database if exists walletname;
  create database walletname;
  \c walletname;
  grant all on database walletname to $POSTGRES_USER;
  create schema if not exists walletname authorization $POSTGRES_USER;
  grant all on schema walletname to $POSTGRES_USER;
EOSQL
