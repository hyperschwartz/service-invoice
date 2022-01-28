-- Blow away a previous instance if it's somehow there
drop database if exists "invoice-db";
create database "invoice-db";
-- Ensure that the root user (which the app uses by default) can talk to the database
grant all on database "invoice-db" to postgres;
-- Main schema that the app is coded to talk to
create schema if not exists invoice authorization postgres;
grant all on schema invoice to postgres;
