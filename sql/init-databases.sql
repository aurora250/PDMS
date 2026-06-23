-- PDM Shard databases for Docker deployment
CREATE DATABASE IF NOT EXISTS pdm_shard_0 DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pdm_shard_1 DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pdm_shard_2 DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pdm_shard_3 DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

-- Copy schema to each shard
USE pdm_shard_0; SOURCE /docker-entrypoint-initdb.d/02-shard-schema.sql;
USE pdm_shard_1; SOURCE /docker-entrypoint-initdb.d/02-shard-schema.sql;
USE pdm_shard_2; SOURCE /docker-entrypoint-initdb.d/02-shard-schema.sql;
USE pdm_shard_3; SOURCE /docker-entrypoint-initdb.d/02-shard-schema.sql;
