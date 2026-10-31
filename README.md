# Smart Parking Management System

**Architecture:** `frontend (HTML/JS)` → REST/JSON → `Spring Boot backend (:8080)` → `MySQL`
and Spring Boot → `Python FastAPI predictor (:8000)` (ML only; no business logic).

## Run
1. MySQL running locally (edit `backend/src/main/resources/application.properties` credentials). DB is auto-created.
2. Predictor: `cd predictor && pip install -r requirements.txt && uvicorn app:app --port 8000`
3. Backend: `cd backend && mvn spring-boot:run` (seeds 2 locations x 10 slots)
4. Frontend: run `python -m http.server 5500` inside `frontend`, then open `http://localhost:5500`.
5. On the login screen, sign in with `user / user123` (driver reservations) or `admin / admin123` (operator functions). These demo accounts are inserted only when the `app_user` table is empty. Do not choose **Preview interface** when testing real reservations: preview uses sample data and intentionally does not call the backend.

## Database (5 tables)
location(1)─(N)slot · vehicle(1)─(N)reservation(N)─(1)slot · vehicle(1)─(N)parking_record(N)─(1)slot

## API
| Method | Path | Purpose |
|---|---|---|
| GET/POST | /api/locations | list / add location |
| GET | /api/locations/{id}/slots | slots + status |
| POST | /api/slots | add slot |
| GET | /api/locations/{id}/available?from=&to= | free slots for a time window |
| POST | /api/vehicles | register vehicle |
| POST/GET/DELETE | /api/reservations | reserve (409 on overlap) / list / cancel |
| POST | /api/entry, /api/exit/{plate} | gate entry; exit computes fee |
| GET | /api/records | entry/exit history |
| GET | /api/admin/dashboard | KPIs |
| GET | /api/admin/predict | 24h demand forecast via Python |

## Key decisions
- Overlap rule: reservation conflicts if `existing.start < new.end AND existing.end > new.start`.
- Fee = ceil(hours) x rate (`parking.rate-per-hour`).
- Predictor: RandomForest on (hour, weekday) -> arrivals; uses a synthetic baseline until >= 24 hours of real data exist.
- Python is isolated behind one endpoint, so the backend works if it is down (predict returns 503).

## Security & roles (added)
HTTP Basic auth against the `app_user` table (BCrypt hashes). Roles:
- **USER** (driver): view slots, register vehicle, reserve/cancel. Self-registration always creates USER.
- **ADMIN** (operator): everything above + gate entry/exit, records, dashboard, prediction, add locations/slots.

Seeded demo accounts: `admin / admin123`, `user / user123` (change before any real deployment).
Auth endpoints: `POST /api/auth/register`, `GET /api/auth/me`.

## Pricing
Each location has its own `ratePerHour` (seed: City Mall 30, Railway Station 20; settable when adding a location).

## Tests
`cd backend && mvn test` — unit tests (Mockito) for overlap rejection, validation, fee rounding, per-location rate, entry conflicts, duplicate plates.
