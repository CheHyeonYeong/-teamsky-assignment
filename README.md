# TeamSky Learning

## Overview

This project implements the core API set for a chapter-based learning platform:

- random unsolved problem delivery per chapter
- problem submission with objective/subjective grading
- submission detail/history lookup
- bookmark and stats support APIs

## Multi-module structure

The build is split into two Gradle modules:

- `app`: Spring Boot entrypoint and runtime configuration/resources
- `core`: domain logic, controllers, services, repositories, entities, and tests

## Design intent

The build keeps the executable Spring Boot boundary in `app` and moves business
logic into `core`.

Inside `core`, the code is separated by bounded context
(`problem`, `submission`, `stats`, `bookmark`, `chapter`, `user`), and each
context is split into four layers:

- `domain`: entities and business state
- `application`: use-case orchestration services
- `infrastructure`: JPA repositories and persistence-specific code
- `presentation`: controllers and request/response DTOs

This is intentionally lighter than a full ports-and-adapters setup, but it
still makes the dependency direction visible and keeps the assignment scope
practical.

The main state/aggregation design choices are:

- `user_problem_state`: keeps the latest per-user/per-problem result and avoids
  scanning the full `submissions` table for random unsolved or wrong-problem
  retrieval.
- `user_chapter_state`: stores the most recently skipped problem per
  user/chapter so the next random fetch can exclude only the immediate skip.
- `user_submission_stats` and `user_chapter_submission_stats`: keep user-level
  dashboard stats cheap to read.
- `problem_stats`: stores per-problem correct-rate counters using the first
  submission from each user. This matches the assignment requirement of
  "30+ users", while still keeping reads O(1).

## Requirement-specific decisions

- Multiple-choice partial credit follows the assignment rule exactly:
  selecting at least one correct choice without matching the full answer set
  returns `PARTIAL`.
- Problem correct rate treats `PARTIAL` as wrong.
- Problem correct rate is exposed only when at least 30 distinct users have
  submitted that problem.
- "No more problems" is returned as `404 NOT_FOUND` through a dedicated business
  error code.
- Pagination responses use a stable `PageResponse<T>` wrapper instead of
  returning Spring's `Page` JSON shape directly.

## Performance notes

- `ORDER BY RAND()` was removed from random-problem selection.
- Random selection now works on filtered candidate counts plus indexed paging.
- Submission/history/stats bottlenecks were reduced with summary tables and DTO
  projections.
- The repository is MySQL-first. Tests run against MySQL as well, not H2.

## API docs

Swagger UI is available at:

- `/swagger-ui.html`
- `/api-docs`

## Local run

The default profile keeps `spring.jpa.hibernate.ddl-auto=validate`.
For local development, use the `local` profile to allow schema updates.

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\scripts\gradlew-java21.ps1 :app:bootRun
```

The local profile reads these variables first and falls back to local defaults:

- `LOCAL_DB_URL`
- `LOCAL_DB_USERNAME`
- `LOCAL_DB_PASSWORD`

Default local database:

```text
jdbc:mysql://localhost:3306/learning?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

## Docker Compose

Start the application and MySQL together:

```powershell
docker compose up -d --build
```

Default app endpoint:

- `http://localhost:8080`

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
