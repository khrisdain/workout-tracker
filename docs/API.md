# Nutrition Service — REST API reference

The nutrition microservice exposes one resource, `MealPlan`, at
`http://localhost:8081/api/meal-plans`. Every endpoint requires HTTP Basic
authentication.

| Account | Password | Authorities |
|---|---|---|
| `pulsetrack-app` | `app-secret` | `ROLE_SERVICE` — full CRUD on meal plans |
| `nutrition-admin` | `admin-secret` | `ROLE_SERVICE`, `ROLE_ADMIN` — the above plus actuator |

Credentials are configured in `application.yml` under `nutrition.security.*` and can be
overridden with the `SERVICE_USER`, `SERVICE_PASSWORD`, `ADMIN_USER` and
`ADMIN_PASSWORD` environment variables.

---

## Endpoint summary

| Method | Path | Success | Failure |
|---|---|---|---|
| `GET` | `/api/meal-plans` | `200` | `401` |
| `GET` | `/api/meal-plans/{id}` | `200` | `401`, `404` |
| `POST` | `/api/meal-plans` | `201` + `Location` | `400`, `401` |
| `PUT` | `/api/meal-plans/{id}` | `200` | `400`, `401`, `404` |
| `DELETE` | `/api/meal-plans/{id}` | `204` | `401`, `404` |
| `GET` | `/api/meal-plans/search` | `200` | `400`, `401` |
| `GET` | `/api/meal-plans/stats` | `200` | `401` |

---

## Data types

### `NutritionGoal`

| Value | Calorie multiplier | Protein per kg | Fat share |
|---|---|---|---|
| `CUT` | 0.82 | 2.2 g | 25% |
| `MAINTAIN` | 1.00 | 1.8 g | 28% |
| `BULK` | 1.12 | 2.0 g | 25% |

### `ActivityLevel`

| Value | Multiplier |
|---|---|
| `SEDENTARY` | 1.20 |
| `LIGHT` | 1.375 |
| `MODERATE` | 1.55 |
| `HIGH` | 1.725 |
| `ATHLETE` | 1.90 |

### `BiologicalSex`

`FEMALE` (−161), `MALE` (+5), `UNSPECIFIED` (−78). Used only as the constant term in
the Mifflin-St Jeor equation; the field is optional and defaults to `UNSPECIFIED`.

---

## Request body

Used by both `POST` and `PUT`.

```json
{
  "memberUsername": "jordan",
  "memberName": "Jordan Reyes",
  "goal": "CUT",
  "activityLevel": "HIGH",
  "weightKg": 78.4,
  "heightCm": 176,
  "age": 27,
  "sex": "UNSPECIFIED",
  "weeklyTrainingMinutes": 240,
  "notes": "Half marathon block"
}
```

| Field | Required | Constraint |
|---|---|---|
| `memberUsername` | yes | not blank, ≤ 40 characters |
| `memberName` | no | ≤ 120 characters |
| `goal` | yes | `CUT` \| `MAINTAIN` \| `BULK` |
| `activityLevel` | yes | one of the five values above |
| `weightKg` | yes | 30 – 300 |
| `heightCm` | yes | 100 – 250 |
| `age` | yes | 13 – 100 |
| `sex` | no | defaults to `UNSPECIFIED` |
| `weeklyTrainingMinutes` | no | 0 – 2000, defaults to 0 |
| `notes` | no | ≤ 300 characters |

---

## Response body

```json
{
  "id": 6,
  "memberUsername": "jordan",
  "memberName": "Jordan Reyes",
  "goal": "CUT",
  "activityLevel": "HIGH",
  "weightKg": 78.4,
  "heightCm": 176,
  "age": 27,
  "weeklyTrainingMinutes": 240,
  "basalMetabolicRate": 1571,
  "maintenanceCalories": 2881,
  "dailyCalories": 2363,
  "proteinGrams": 172,
  "carbGrams": 254,
  "fatGrams": 66,
  "hydrationLitres": 3.0,
  "summary": "2363 kcal a day, a deficit against an estimated 2881 kcal maintenance. …",
  "notes": "Half marathon block",
  "createdAt": "2026-08-18T11:42:36.604415",
  "updatedAt": "2026-08-18T11:42:36.604415"
}
```

The six computed fields (`basalMetabolicRate` through `hydrationLitres`) are always
derived by the server. Sending them in a request body has no effect — a `PUT`
recalculates the plan from scratch, so a stored plan can never drift out of step with
its own inputs.

---

## `GET /api/meal-plans/search`

The custom multi-parameter endpoint. All four parameters are optional and combine with
`AND`; omitting one drops that predicate entirely.

| Parameter | Type | Meaning |
|---|---|---|
| `member` | string | exact username, case-insensitive |
| `goal` | enum | `CUT`, `MAINTAIN` or `BULK` |
| `minCalories` | integer | `dailyCalories >= minCalories` |
| `maxCalories` | integer | `dailyCalories <= maxCalories` |

Results are ordered newest first. A search that matches nothing returns `200` with an
empty array, never `404`.

```bash
curl -u pulsetrack-app:app-secret \
  'http://localhost:8081/api/meal-plans/search?member=priya&goal=CUT&minCalories=1500&maxCalories=2500'
```

---

## `GET /api/meal-plans/stats`

```json
{
  "totalPlans": 5,
  "distinctMembers": 5,
  "averageDailyCalories": 2709,
  "averageProteinGrams": 149,
  "plansByGoal": { "BULK": 1, "CUT": 2, "MAINTAIN": 2 }
}
```

This is what the primary application's `/admin` console renders in its "Nutrition
microservice" panel, beside the figures it reads from its own database.

---

## Error responses

Every failure returns the same shape.

**`401` — no or wrong credentials**

```
WWW-Authenticate: Basic realm="Realm"
```

**`400` — validation failure**

```json
{
  "timestamp": "2026-08-18T11:44:02.118",
  "status": 400,
  "error": "Bad Request",
  "message": "The request body failed validation",
  "path": "/api/meal-plans",
  "fieldErrors": {
    "memberUsername": "memberUsername is required",
    "goal": "goal is required (CUT, MAINTAIN or BULK)"
  }
}
```

**`404` — unknown id**

```json
{
  "timestamp": "2026-08-18T11:44:11.902",
  "status": 404,
  "error": "Not Found",
  "message": "No meal plan exists with id 999999",
  "path": "/api/meal-plans/999999"
}
```

**`500`** returns the same envelope with a generic message. The stack trace is logged
on the server and never sent to the caller.

---

## The calculation

Given weight `w` kg, height `h` cm, age `a`, sex constant `s`, activity multiplier `m`,
weekly training minutes `t`, and a goal with multiplier `g`, protein rate `p` and fat
share `f`:

```
bmr          = 10w + 6.25h − 5a + s
maintenance  = bmr × m + (t × 5) ÷ 7
target       = max(maintenance × g, bmr × 1.10)
proteinGrams = w × p
fatGrams     = (target × f) ÷ 9
carbGrams    = max(0, (target − proteinGrams×4 − fatGrams×9) ÷ 4)
hydration    = w × 0.033 + (t ÷ 7) × 0.012
```

`weeklyTrainingMinutes` is what makes the two applications genuinely coupled: PulseTrack
sums the member's last seven days of logged sessions and sends that number along, so a
member who trained hard last week gets a higher calorie target than an identical member
who did not.

The `max(…, bmr × 1.10)` floor is the reason a `CUT` for a small, sedentary member can
come back at maintenance rather than below it — see `MacroCalculatorTest`.
