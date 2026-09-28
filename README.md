# RideShareLife

A small Spring Boot REST backend for matching office and college carpools. It uses Spring Web, Spring Data JPA, Jakarta validation, and H2 by default; the MySQL JDBC driver is included for a real database.

## Requirements

- Java 21+
- Maven 3.9+
- H2 is file-backed by default, so no database installation is needed for a local demo. Run one app instance at a time while using the local database.

## Run

From this directory:

```sh
mvn spring-boot:run
```

To use MySQL, create a database named `ridesharelife`, then set `DB_URL=jdbc:mysql://localhost:3306/ridesharelife`, `DB_USERNAME`, and `DB_PASSWORD` before starting the same command. Hibernate creates/updates tables on startup. For a deployed system, set `spring.jpa.hibernate.ddl-auto=validate` and manage schema changes with migrations.

Base URL: `http://localhost:8080/api`. Times use ISO local date-time, for example `2026-10-01T08:30:00`.

## Dashboard

The responsive web dashboard is served by Spring Boot at `http://localhost:8080/` (or the port configured for the app). It supports profile selection, ride publishing and search, seat requests, driver decisions, and ride history. Profiles created in the dashboard are remembered in that browser; the profile dialog can also load an existing database user by ID. The H2 console remains available at `/h2-console` during local development.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/users` | Register a user: `{ "name":"Asha", "email":"asha@example.com" }` |
| GET | `/users/{id}` | View a user |
| POST | `/rides` | Publish a ride: `{ "driverId":1, "origin":"Pune", "destination":"Mumbai", "departureTime":"2026-10-01T08:30:00", "seats":3 }` |
| GET | `/rides/search?origin=Pune&destination=Mumbai&from=2026-10-01T07:30:00&to=2026-10-01T09:30:00` | Find same-route rides with seats within the inclusive time window |
| POST | `/requests` | Request a seat: `{ "riderId":2, "rideOfferId":1 }` |
| PUT | `/requests/{id}/decision` | Driver decision: `{ "driverId":1, "status":"APPROVED" }` or `REJECTED` |
| GET | `/users/{id}/history` | See rides published, requests made as a rider, and requests received as a driver |

Successful creates return `201`; reads and updates return `200`. Validation/business errors return a JSON `{ "error": "..." }` with `400`, missing records `404`, and conflicting data `409`.

## Business rules

- Drivers publish a route, departure time and 1–20 seats.
- Riders can request each ride once, but cannot request their own ride.
- Only the ride's driver can approve or reject a pending request.
- Approved requests atomically reserve a seat; approval fails with a clear error once seats run out. Rejected requests do not affect seats.
- Search is case-insensitive on origin/destination and includes both ends of the time window.

The demo API uses user IDs as identity references and intentionally does not add authentication, payments, maps, or a frontend.
