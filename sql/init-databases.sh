#!/bin/bash
set -e

echo "============================================================"
echo " PDM Database Initialization"
echo "============================================================"

# Create shard databases and seed area data
SHARD_DBS="pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3"

for db in $SHARD_DBS; do
    echo "Creating database: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE DATABASE "$db";
EOSQL
done

echo "All shard databases created successfully."

# Apply shard schema and area seed to each shard database
for db in $SHARD_DBS; do
    echo "Applying shard schema to: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f /docker-entrypoint-initdb.d/02-init-shard-schema.sql
    echo "Seeding area data to: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f /docker-entrypoint-initdb.d/03-init-area-data.sql
done

echo "============================================================"
echo " Database initialization complete!"
echo " Main DB: pdm_db (24 tables + area seed)"
echo " Shard DBs: $SHARD_DBS (7 shard tables + 2 broadcast tables)"
echo "============================================================"
