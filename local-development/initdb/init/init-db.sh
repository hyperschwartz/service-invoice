#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 -h$PG_HOST --username "$PG_USER" <<-EOSQL
    /* Method 1: update system catalog */
    UPDATE pg_database
      SET datallowconn = 'false'
      WHERE datname = '$1';
    SELECT pg_terminate_backend(pid)
      FROM pg_stat_activity
      WHERE datname = '$1';
    DROP DATABASE IF EXISTS "$1";
    CREATE DATABASE "$1";
    GRANT ALL ON DATABASE "$1" TO $PG_USER;
    \c $1;
    CREATE SCHEMA "$1";
    GRANT ALL ON SCHEMA "$1" TO $PG_USER;
    ALTER DATABASE "$1" SET search_path TO public,"$1";
EOSQL
