#!/bin/bash
set -e

# Create shard databases
# PostgreSQL Docker entrypoint runs *.sh files before *.sql files
# $POSTGRES_DB is pdm_db (already exists), we need to create the shard DBs

for db in pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3; do
    echo "Creating database: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE DATABASE "$db";
EOSQL
done

# Apply shard schema to each shard database
for db in pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3; do
    echo "Applying shard schema to: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f /docker-entrypoint-initdb.d/02-init-shard-schema.sql
done
