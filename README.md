# TeamSky Learning

## Local run

The default profile keeps `spring.jpa.hibernate.ddl-auto=validate`.
For local development, use the `local` profile to allow schema updates.

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\scripts\gradlew-java21.ps1 bootRun
```

The local profile reads these variables first and falls back to local defaults:

- `LOCAL_DB_URL`
- `LOCAL_DB_USERNAME`
- `LOCAL_DB_PASSWORD`

Default local database:

```text
jdbc:mysql://localhost:3306/learning?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

## Tests

Run the full suite with Java 21:

```powershell
.\scripts\gradlew-java21.ps1 test
```

Test profile database variables:

- `TEST_DB_URL`
- `TEST_DB_USERNAME`
- `TEST_DB_PASSWORD`

## Existing database migration

If you already have a MySQL database from an older schema version, run the
rerunnable migration before starting the default profile. It creates the new
state/aggregate tables and backfills them from `submissions`.

```powershell
mysql --host localhost --port 3306 --user root --password=root learning < scripts/migrations/20260325_backfill_state_and_stats.sql
```

The migration also imports the latest per-chapter skip state from
`skipped_problems` when that legacy table exists.

## Load test

Start the app first, then run:

```powershell
python scripts/load_test.py --scenario mixed --base-url http://localhost:8080 --requests 10000 --concurrency 250 --timeout 10
```

Useful scenarios:

- `mixed`
- `read-problem`
- `read-history`
- `read-wrong`
- `read-user-stats`
- `write-subjective`
