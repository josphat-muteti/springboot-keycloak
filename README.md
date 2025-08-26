# Spring Boot + Keycloak (Multi-Realm)

A Spring Boot REST API secured by Keycloak. Each organization maps 1:1 to a Keycloak realm.  
Organizations are stored in PostgreSQL; users live in Keycloak.  
Emails are sent on user create/update/delete/enable/disable (Mailhog for local).

## Stack
- Spring Boot 3.x (Java 21), JPA (Hibernate)
- Keycloak 25 (admin REST API)
- PostgreSQL 16
- Mailhog (SMTP for local testing)
- Docker & docker-compose

---

## Quick Start (Local, with Docker)
```bash
cd docker
# Windows:
copy .env.example .env

docker compose up --build


URLs
Keycloak Admin Console → http://localhost:8081
 (admin / admin)
API (Spring Boot) → http://localhost:8080
Mailhog (emails) → http://localhost:8025


Security (JWT / Roles)

All /api/** endpoints require a bearer token whose realm roles include admin.

Get a token from Keycloak’s master realm (local dev):

Windows PowerShell

$resp = Invoke-RestMethod -Method POST `
  -Uri http://localhost:8081/realms/master/protocol/openid-connect/token `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin"
$TOKEN = $resp.access_token


Use the token:

Authorization: Bearer <TOKEN>

Endpoints
Organizations (DB: Postgres)

POST /api/organizations — create an organization (also ensures a Keycloak realm with same name)

GET /api/organizations — list organizations

GET /api/organizations/{id} — get one

PUT /api/organizations/{id} — update description / enabled

DELETE /api/organizations/{id} — delete (does not delete the Keycloak realm by design)

POST /api/organizations/{id}/enable — set enabled=true (also enables the realm)

POST /api/organizations/{id}/disable — set enabled=false (also disables the realm)

Users (Keycloak, per realm)

POST /api/realms/{realm}/users — create user (sets temporary password, sends email)

GET /api/realms/{realm}/users — list

GET /api/realms/{realm}/users/{userId} — get

PUT /api/realms/{realm}/users/{userId} — update (sends email)

DELETE /api/realms/{realm}/users/{userId} — delete (sends email)

POST /api/realms/{realm}/users/{userId}/enable — enable (sends email)

POST /api/realms/{realm}/users/{userId}/disable — disable (sends email)

Email Configuration

Local dev uses Mailhog via these env vars (see docker/docker-compose.yml):

SMTP_HOST=smtp
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false


Switch to Gmail/Mailtrap by setting SMTP_* and enabling SMTP_AUTH=true, SMTP_STARTTLS=true.

Database Schema & Sample Data

On first run, Spring auto-creates the table and inserts sample orgs.

src/main/resources/schema.sql

CREATE TABLE IF NOT EXISTS organization (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);


src/main/resources/data.sql

INSERT INTO organization (name, description, enabled) VALUES
  ('acme', 'Acme Corporation', true),
  ('globex', 'Globex Corp', true)
ON CONFLICT (name) DO NOTHING;

ERD
erDiagram
  ORGANIZATION {
    bigint id PK
    varchar name "unique, not null"  // == Keycloak realm
    text description
    boolean enabled
  }


Users are in Keycloak, not in our DB.

Smoke Test (powershell, using curl)

Get token (see Security section).

Create org (ensures realm):

curl -X POST http://localhost:8081/api/organizations \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"serianu","description":"Security Company","enabled":true}'


Create user (watch Mailhog for email):

curl -X POST http://localhost:8081/api/realms/serianu/users \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"josphat.muteti","email":"josphat@example.com","firstName":"Josphat","lastName":"Muteti","temporaryPassword":"Temp#1234"}'


List users:

curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/realms/serianu/users


Disable/Enable/Delete user (check Mailhog after each):

curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/realms/serianu/users/<USER_ID>/disable
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/realms/serianu/users/<USER_ID>/enable
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/realms/acme/serianu/<USER_ID>

Notes

Organization name = Keycloak realm (kept identical).

Disabling an org toggles the Keycloak realm’s enabled flag.

Realm deletion is deliberately not automated to avoid destructive mistakes.

Docker Services

postgres (port 5432)

keycloak (port 8081 on host)

smtp Mailhog (ports 1025/8025)

app Spring Boot API (port 8080)


---


mkdir scripts

**File:** `scripts\smoke-test.ps1`
```powershell
# Get token
$resp = Invoke-RestMethod -Method POST `
  -Uri http://localhost:8081/realms/master/protocol/openid-connect/token `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin"
$TOKEN = $resp.access_token
Write-Host "Token acquired`n"

# Create organization
Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/organizations `
  -Headers @{ "Authorization"="Bearer $TOKEN"; "Content-Type"="application/json" } `
  -Body '{"name":"serianu","description":"Security Company","enabled":true}' | Out-Host

# Create user
$user = Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/realms/serianu/users `
  -Headers @{ "Authorization"="Bearer $TOKEN"; "Content-Type"="application/json" } `
  -Body '{"username":"josphat.muteti","email":"josphat@example.com","firstName":"Josphat","lastName":"Muteti","temporaryPassword":"Temp#1234"}'
$userId = $user.id
Write-Host "User created: $($user.username) id=$userId`n"

# List users
Invoke-RestMethod -Method GET -Uri http://localhost:8081/api/realms/serianu/users `
  -Headers @{ "Authorization"="Bearer $TOKEN" } | Out-Host

# Disable / Enable
Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/realms/serianu/users/$userId/disable `
  -Headers @{ "Authorization"="Bearer $TOKEN" }
Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/realms/serianu/users/$userId/enable `
  -Headers @{ "Authorization"="Bearer $TOKEN" }

# Delete
Invoke-RestMethod -Method DELETE -Uri http://localhost:8081/api/realms/serianu/users/$userId `
  -Headers @{ "Authorization"="Bearer $TOKEN" }
Write-Host "`nDone. Check Mailhog at http://localhost:8025"
