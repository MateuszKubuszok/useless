# Example

Example app that aim to run a primitive transaction with either
Doobie or Slick.

## Usage

Create test database

```sql
CREATE USER useless_user WITH PASSWORD 'pass';
CREATE DATABASE useless OWNER useless_user;
GRANT ALL PRIVILEGES ON DATABASE useless TO useless_user;
ALTER ROLE useless_user CREATEROLE CREATEDB;
```

Pass settings to app with env variables:

```bash
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=useless
export PGUSER=useless_user
export PGPASSWORD=pass
scripts/sbt example/runMain        # run only migration
scripts/sbt example/runMain doobie # tests run with Doobie
scripts/sbt example/runMain slick  # tests run with Slick
```
