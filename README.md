# PulseTrack

> Train with intent. Measure what matters.

PulseTrack is a workout tracking web application built for **CPAN-228 Web Application
Development**. Members log training sessions, follow multi-week programs written by
coaches, and get a nutrition plan calculated by a **second, independent microservice**.
Administrators manage accounts, roles and every record in the system from a console
that reads from both the local database and the remote service at once.

The repository holds two Spring Boot applications:

| Module | Path | Port | What it owns |
|---|---|---|---|
| **PulseTrack Web** (primary) | `./` | `8080` | Members, roles, workouts, programs, all the pages |
| **Nutrition Service** (microservice) | `./nutrition-service/` | `8081` | Meal plans, macro calculation, its own database and its own users |

---

## Table of contents

1. [Quick start](#quick-start)
2. [What the application does](#what-the-application-does)
3. [Demo accounts](#demo-accounts)
4. [Architecture](#architecture)
5. [Configuration and profiles](#configuration-and-profiles)
6. [Running under each profile](#running-under-each-profile)
7. [Docker](#docker)
8. [The REST API](#the-rest-api)
9. [Security model](#security-model)
10. [Testing](#testing)
11. [Project layout](#project-layout)
12. [Team contributions](#team-contributions)

---

## Quick start

Nothing to install beyond a JDK and Maven. The default `dev` profile runs entirely on
in-memory H2 and seeds itself, so a fresh clone shows a populated application.

```bash
git clone https://github.com/khrisdain/workout-tracker.git
cd workout-tracker
```

**Terminal 1 — the nutrition microservice:**

```bash
cd nutrition-service && mvn spring-boot:run
```

**Terminal 2 — the primary application:**

```bash
mvn spring-boot:run
```

Open **<http://localhost:8080>** and sign in as `admin` / `Admin@123`.

> The primary application runs perfectly well on its own. If you skip terminal 1, the
> nutrition pages show a clear "service unavailable" banner and everything else keeps
> working — see [graceful degradation](#graceful-degradation).

**Prerequisites:** JDK 17 or newer (built and verified on 21), Maven 3.9+, and Docker
only if you want the MySQL or PostgreSQL profiles.

---

## What the application does

### For members
- **Log a session** in one short form: name, type, duration, difficulty, date, notes.
  Leave calories blank and PulseTrack estimates them from the activity's MET value,
  your bodyweight and how hard the session felt.
- **Browse the training log** with server-side search, two filter dimensions
  (type, difficulty), a date range, a minimum-duration filter, sortable columns and
  pagination — all pushed into the database, never trimmed in memory.
- **A personal dashboard**: 30-day volume trend, split by workout type, progress
  against a weekly active-minutes target, recent sessions and enrolled programs.
- **Enrol in programs** written by coaches.
- **Request a nutrition plan**, which is calculated by the microservice from the
  member's metrics plus their last seven days of logged training.

### For coaches
Everything a member can do, plus **authoring training programs** (name, description,
focus, difficulty, length in weeks, sessions per week, published or draft) and
reviewing the whole roster's training log.

### For administrators
A dedicated `/admin` console:
- **Users** — change roles, suspend/reactivate, delete. The last remaining
  administrator is protected from all three.
- **Workouts** — filter, edit or delete any session, whoever logged it.
- **Programs** — including drafts.
- **Nutrition service** — browse, search and delete meal plans that live in the other
  application's database, with live connection status.

---

## Demo accounts

Seeded by the profile initialisation script on every `dev` start.

| Username | Password | Roles |
|---|---|---|
| `admin` | `Admin@123` | Administrator + Coach + Member |
| `coach.b` | `Coach@123` | Coach + Member |
| `coach.m` | `Coach@123` | Coach + Member |
| `jordan` | `Member@123` | Member |
| `priya` | `Member@123` | Member |
| `tomas` | `Member@123` | Member |
| `leila` | `Member@123` | Member |

Passwords are stored as BCrypt hashes (strength 10). The plain text above exists only
so a reviewer can sign in; nothing in the database holds a readable password.

The nutrition microservice has its own, separate credentials:

| Username | Password | Can reach |
|---|---|---|
| `pulsetrack-app` | `app-secret` | `/api/**` — the identity the primary app authenticates with |
| `nutrition-admin` | `admin-secret` | `/api/**` plus the actuator endpoints |

---

## Architecture

```
                    browser
                       │
                       ▼
        ┌──────────────────────────────┐
        │   PulseTrack Web  :8080      │
        │                              │
        │  Thymeleaf + Bootstrap 5     │
        │  Spring MVC controllers      │
        │  Spring Security (3 roles)   │
        │  Spring Data JPA             │
        └───────┬──────────────┬───────┘
                │              │
        JPA     │              │  RestTemplate + HTTP Basic
                ▼              ▼
        ┌───────────────┐   ┌──────────────────────────────┐
        │  H2  (dev)    │   │  Nutrition Service  :8081    │
        │  MySQL (prod) │   │                              │
        │               │   │  REST controller             │
        │  users        │   │  MacroCalculator             │
        │  user_roles   │   │  Spring Security (Basic)     │
        │  workouts     │   │  Spring Data JPA             │
        │  programs     │   └──────────┬───────────────────┘
        │  program_...  │              │
        └───────────────┘              ▼
                              ┌──────────────────────┐
                              │  H2 (dev)            │
                              │  PostgreSQL (qa)     │
                              │                      │
                              │  meal_plans          │
                              └──────────────────────┘
```

### Why the split is real, not cosmetic

The nutrition service is **not a pass-through**. It owns a resource the primary
application never stores, and it applies genuine domain logic to produce it:

1. **Resting energy** from the Mifflin-St Jeor equation
   (`10 × kg + 6.25 × cm − 5 × age + sex constant`).
2. **Maintenance energy** — resting scaled by an activity multiplier, plus a bonus
   derived from the training minutes PulseTrack reports for that member.
3. **A goal target** — maintenance scaled by the goal multiplier (cut / maintain /
   bulk), floored at 110% of resting so an aggressive cut can never become dangerous.
4. **A macro split** — protein from bodyweight, fat as a share of calories,
   carbohydrate from whatever energy is left.

The two applications share no jar, no schema and no user table. They exchange JSON
over HTTP, and each has its own copy of the small enum contract on purpose, so either
can be redeployed without the other.

### How the primary app consumes it

`NutritionServiceClient` is the single integration point. Every call is funnelled
through one method that catches connection failures, timeouts, 401/403, 404, 4xx and
5xx, and converts each into a `RemoteServiceStatus` the UI can render.

#### Graceful degradation

Stop the microservice and reload `/nutrition` or `/admin`. Instead of a stack trace
you get a warning banner naming the endpoint and telling you how to start the service,
and every other page in PulseTrack keeps working normally. Connect and read timeouts
are 2s and 4s, so a hung service never blocks a request thread long enough to make
PulseTrack itself feel broken.

---

## Configuration and profiles

**There is no `application.properties` anywhere in this repository.** Every setting
lives in hierarchical YAML, and switching environments never requires a source change.

### Primary application — `src/main/resources/`

| File | Purpose |
|---|---|
| `application.yml` | Common to every environment: app name, MVC, actuator, branding, weekly goal, and the nutrition service connection block |
| `application-dev.yml` | **dev** — in-memory H2, `ddl-auto: create-drop`, H2 console at `/h2-console`, SQL logging, seeds on every start |
| `application-prod.yml` | **prod** — MySQL through `${DB_*}` placeholders, connection pool sizing, template caching on, H2 console explicitly disabled, file logging |
| `data-h2.sql` | Initialisation script for the dev profile |
| `data-mysql.sql` | Initialisation script for the prod profile |

### Nutrition service — `nutrition-service/src/main/resources/`

| File | Purpose |
|---|---|
| `application.yml` | Common: port, Jackson, actuator, and the Basic Auth accounts |
| `application-dev.yml` | **dev** — in-memory H2, console enabled, seeds on every start |
| `application-qa.yml` | **qa** — PostgreSQL from Docker through `${DB_*}` placeholders |
| `data-h2.sql` | Initialisation script for the dev profile |
| `data-postgresql.sql` | Initialisation script for the qa profile, written idempotently so a restart against the persistent database never duplicates rows |

Each profile resolves its own seed script through `spring.sql.init.platform`, which is
why the two files can use their own dialect's date functions and keep the sample data
relative to the day you run the app.

### Environment variables

Every value below has a working default, so nothing has to be exported to run the
project. Copy `.env.example` to `.env` to override them for Docker.

**Primary application**

| Variable | Default | Used by |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | profile selection |
| `SERVER_PORT` | `8080` | both profiles |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `pulsetrack` | prod |
| `DB_USERNAME` / `DB_PASSWORD` | `pulsetrack` / `root` | prod |
| `DDL_AUTO` | `update` | prod |
| `SQL_INIT_MODE` | `never` | prod — set to `always` for the first run |
| `NUTRITION_SERVICE_URL` | `http://localhost:8081` | both |
| `NUTRITION_SERVICE_USER` / `NUTRITION_SERVICE_PASSWORD` | `pulsetrack-app` / `app-secret` | both |
| `NUTRITION_SERVICE_ENABLED` | `true` | both — set `false` to switch the integration off |

**Nutrition service**

| Variable | Default |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `SERVER_PORT` | `8081` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `nutritiondb` |
| `DB_USERNAME` / `DB_PASSWORD` | `nutrition` / `nutrition-pass` |
| `SERVICE_USER` / `SERVICE_PASSWORD` | `pulsetrack-app` / `app-secret` |
| `ADMIN_USER` / `ADMIN_PASSWORD` | `nutrition-admin` / `admin-secret` |

---

## Running under each profile

The active profile is shown in the footer of every page and on the About page, so you
can confirm at a glance which database you are talking to.

### dev — H2 in memory (the default)

No containers, no setup. The schema is created and seeded on every start, so the data
is identical each time.

```bash
mvn spring-boot:run
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

H2 console: <http://localhost:8080/h2-console> · JDBC URL `jdbc:h2:mem:pulsetrackdb` ·
user `sa` · no password.

### prod — MySQL

Start the database first, then pick the profile. Any of these three forms works, and
none of them touches source code:

```bash
docker compose up -d mysql
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

```bash
java -Dspring.profiles.active=prod -jar target/workout-tracker-1.0.0.jar
```

```bash
java -jar target/workout-tracker-1.0.0.jar --spring.profiles.active=prod
```

**Schema and first run.** Hibernate creates the tables from the entities
(`ddl-auto: update`), so no manual DDL is needed. The seed script is opt-in on MySQL
because the database is persistent — run it once against an empty schema:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod -Dspring-boot.run.arguments=--spring.sql.init.mode=always
```

After that first run, start normally and the existing data is kept.

Using your own MySQL instead of the container:

```bash
DB_HOST=localhost DB_PORT=3306 DB_NAME=pulsetrack DB_USERNAME=root DB_PASSWORD=secret \
  java -Dspring.profiles.active=prod -jar target/workout-tracker-1.0.0.jar
```

### Nutrition service: dev and qa

```bash
cd nutrition-service && mvn spring-boot:run
```

```bash
docker compose up -d postgres
cd nutrition-service && mvn spring-boot:run -Dspring-boot.run.profiles=qa
```

---

## Docker

`docker-compose.yml` brings up everything the non-default profiles need. The
applications themselves run on the host; only the databases are containerised.

```bash
docker compose up -d            # MySQL + PostgreSQL + Adminer
docker compose up -d mysql      # just the primary app's prod database
docker compose up -d postgres   # just the microservice's qa database
docker compose ps               # check health
docker compose logs -f mysql
docker compose down             # stop, keep the data
docker compose down -v          # stop and wipe the volumes
```

| Service | Host port | Credentials |
|---|---|---|
| `mysql` (8.4) | `3306` | db `pulsetrack`, user `pulsetrack`, password `root` |
| `postgres` (16-alpine) | `5432` | db `nutritiondb`, user `nutrition`, password `nutrition-pass` |
| `adminer` | `8082` | browse either database at <http://localhost:8082> |

Both database services declare health checks, so `docker compose ps` tells you when
they are actually ready to accept a connection rather than merely started.

### Full system in one pass

```bash
docker compose up -d
cd nutrition-service && mvn -q clean package -DskipTests
java -Dspring.profiles.active=qa -jar target/nutrition-service-1.0.0.jar &
cd .. && mvn -q clean package -DskipTests
java -Dspring.profiles.active=prod -jar target/workout-tracker-1.0.0.jar --spring.sql.init.mode=always
```

---

## The REST API

Base URL `http://localhost:8081`. Every route requires HTTP Basic credentials.

| Method | Path | Success | Notes |
|---|---|---|---|
| `GET` | `/api/meal-plans` | `200` | Every plan, newest first |
| `GET` | `/api/meal-plans/{id}` | `200` | `404` when the id is unknown |
| `POST` | `/api/meal-plans` | `201` | `Location` header set; macros computed server-side |
| `PUT` | `/api/meal-plans/{id}` | `200` | Recalculates from the new inputs; `404` when unknown |
| `DELETE` | `/api/meal-plans/{id}` | `204` | `404` when unknown |
| `GET` | `/api/meal-plans/search` | `200` | **Custom multi-parameter query**: `member`, `goal`, `minCalories`, `maxCalories`, any subset |
| `GET` | `/api/meal-plans/stats` | `200` | Aggregates for the admin dashboard |
| `GET` | `/actuator/health` | `200` | Open |
| `GET` | `/actuator/env` | `200` | `ROLE_ADMIN` only — `403` for the service account |

Failures come back as a consistent JSON body: `401` without credentials, `400` with a
`fieldErrors` map when validation fails, `404` for an unknown id, `500` with no stack
trace leaked.

### Try it

```bash
curl -u pulsetrack-app:app-secret http://localhost:8081/api/meal-plans
```

```bash
curl -u pulsetrack-app:app-secret -X POST http://localhost:8081/api/meal-plans \
  -H 'Content-Type: application/json' \
  -d '{"memberUsername":"jordan","memberName":"Jordan Reyes","goal":"CUT","activityLevel":"HIGH","weightKg":78.4,"heightCm":176,"age":27,"sex":"UNSPECIFIED","weeklyTrainingMinutes":240}'
```

```bash
curl -u pulsetrack-app:app-secret 'http://localhost:8081/api/meal-plans/search?goal=CUT&minCalories=1500&maxCalories=3000'
```

```bash
curl -u pulsetrack-app:app-secret http://localhost:8081/api/meal-plans/stats
```

```bash
curl -i http://localhost:8081/api/meal-plans
```

---

## Security model

Three roles, each of which actually changes what the application will let you do.

| | Member | Coach | Administrator |
|---|---|---|---|
| Browse the log and programs | ✅ | ✅ | ✅ |
| Log a workout | ✅ | ✅ | ✅ |
| Edit or delete **their own** session | ✅ | ✅ | ✅ |
| Edit or delete **anyone's** session | ❌ | ✅ | ✅ |
| Request a nutrition plan | ✅ | ✅ | ✅ |
| Create or edit a program | ❌ | ✅ (their own) | ✅ (any) |
| `/admin` console, roles, account deletion | ❌ | ❌ | ✅ |

- **Registration** encodes with BCrypt strength 10 and never stores or logs the plain
  password. New accounts start as `MEMBER`.
- **`User` implements `UserDetails`** directly, so Spring Security authenticates
  against the same row the application reads for profile data.
- **Public surface:** home, about, guide, the read-only log and program listings,
  login and registration. Everything that creates, edits or deletes requires a session.
- **Ownership is checked in the service layer as well as the filter chain**, so a
  member cannot edit another member's session by guessing a URL.
- **The last administrator is protected** from role removal, suspension and deletion.
- **CSRF protection is on** for all form posts. It is switched off only on the
  microservice, which is a stateless API with no browser session to ride.
- **The H2 console is dev-only** — it has its own filter chain under `@Profile("dev")`
  and is explicitly disabled in `application-prod.yml`.
- A styled `/login` page reports bad credentials in plain language, and `403` renders
  a branded access-denied page that names the role you are signed in with.

---

## Testing

```bash
mvn test                            # primary application - 41 tests
cd nutrition-service && mvn test    # microservice        - 30 tests
```

**Primary application (41):** YAML property binding, calorie estimation, ownership
rules, filter/sort/paging behaviour including rejection of an unknown sort field,
BCrypt encoding, duplicate account handling, last-administrator protection, page
rendering, route protection per role, and every server-side validation rule. Plus six
tests that stub the transport to force each microservice failure mode and assert the
application degrades instead of throwing.

**Microservice (30):** the Mifflin-St Jeor arithmetic against known reference values,
the calorie floor, macro reconciliation, and full API contract tests covering CRUD
status codes, the `Location` header, Basic Auth rejection, role separation on the
actuator, validation error bodies and the multi-parameter search endpoint.

---

## Project layout

```
workout-tracker/
├── pom.xml                          primary application
├── docker-compose.yml               MySQL + PostgreSQL + Adminer
├── .env.example
├── README.md
├── docs/
│   └── API.md                       full REST reference
│
├── src/main/java/com/fitnesstracker/workouttracker/
│   ├── WorkoutTrackerApplication.java
│   ├── client/       NutritionServiceClient          RestTemplate + error handling
│   ├── config/       SecurityConfig, DevSecurityConfig, PasswordConfig,
│   │                 RestClientConfig, PulseTrackProperties
│   ├── controller/   Home, Auth, Workout, Program, Dashboard, Nutrition, Admin,
│   │                 GlobalModelAdvice, GlobalExceptionHandler
│   ├── dto/          filters, forms, remote payloads, dashboard projections
│   ├── exception/    ResourceNotFoundException, DuplicateAccountException
│   ├── model/        User, Role, Workout, Program, WorkoutType, DifficultyLevel,
│   │                 NutritionGoal, ActivityLevel, BiologicalSex
│   ├── repository/   Workout (+ Specifications), User, Program
│   └── service/      WorkoutService, UserService, ProgramService, DashboardService
│
├── src/main/resources/
│   ├── application.yml, application-dev.yml, application-prod.yml
│   ├── data-h2.sql, data-mysql.sql
│   ├── static/css/pulsetrack.css, static/js/pulsetrack.js
│   └── templates/    fragments/, auth/, workouts/, programs/, admin/, error/
│
└── nutrition-service/
    ├── pom.xml
    └── src/main/
        ├── java/com/pulsetrack/nutrition/
        │   ├── config/      SecurityConfig, DevConsoleConfig, NutritionProperties
        │   ├── controller/  MealPlanController, ApiExceptionHandler
        │   ├── dto/         MealPlanRequest/Response, NutritionStats, ApiError
        │   ├── model/       MealPlan, NutritionGoal, ActivityLevel, BiologicalSex
        │   ├── repository/  MealPlanRepository
        │   └── service/     MacroCalculator, MealPlanService
        └── resources/
            ├── application.yml, application-dev.yml, application-qa.yml
            └── data-h2.sql, data-postgresql.sql
```

---

## Team contributions

<!-- Replace the names below with the real team members before submitting. -->

| Team member | Deliverable 1 | Deliverable 2 | Deliverable 3 |
|---|---|---|---|
| _name_ | Workout entity, repository, form and validation | Registration flow and BCrypt encoding | Nutrition microservice REST API |
| _name_ | Thymeleaf templates, Bootstrap layout, home and informational pages | Custom login page and protected routes | YAML profile split and Docker Compose |
| _name_ | List view filtering, sorting and pagination | Role model and admin console | `RestTemplate` client, error handling, admin dashboard |
| _name_ | Seed data and dashboards | Program authoring and coach permissions | Test suites and documentation |

---

## Tech stack

Java 17 · Spring Boot 3.3.5 · Spring MVC · Spring Data JPA · Spring Security 6 ·
Spring Validation · Spring Boot Actuator · Thymeleaf + `thymeleaf-extras-springsecurity6` ·
Bootstrap 5.3 · Bootstrap Icons · Chart.js 4 · H2 · MySQL 8.4 · PostgreSQL 16 ·
Docker Compose · Maven · JUnit 5 · AssertJ · MockMvc · `MockRestServiceServer`
