#!/bin/bash
set -e

echo "============================================================"
echo " PDM Database Initialization"
echo "============================================================"

# Create shard databases
# PostgreSQL Docker entrypoint runs *.sh files before *.sql files
# $POSTGRES_DB is pdm_db (already exists), we need to create the shard DBs

SHARD_DBS="pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3"

for db in $SHARD_DBS; do
    echo "Creating database: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE DATABASE "$db";
EOSQL
done

echo "All shard databases created successfully."

# Apply shard schema to each shard database
for db in $SHARD_DBS; do
    echo "Applying shard schema to: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f /docker-entrypoint-initdb.d/02-init-shard-schema.sql
done

echo "============================================================"
echo " Database initialization complete!"
echo " Main DB: pdm_db"
echo " Shard DBs: $SHARD_DBS"
echo "============================================================"
