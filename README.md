# Spring Boot + Keycloak (Multi-Realm).

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
Keycloak Admin Console → http://localhost:8081 (admin / admin)

API (Spring Boot) → http://localhost:8080

Mailhog (emails) → http://localhost:8025

Security (JWT / Roles)
All /api/** endpoints require a bearer token whose realm roles include admin.

Get a token from Keycloak’s master realm (local dev):

Windows PowerShell

powershell
$resp = Invoke-RestMethod -Method POST `
  -Uri http://localhost:8081/realms/master/protocol/openid-connect/token `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin"
$TOKEN = $resp.access_token
Use the token header:

makefile

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

ini

SMTP_HOST=smtp
SMTP_PORT=1025
SMTP_AUTH=false
SMTP_STARTTLS=false
Switch to Gmail/Mailtrap by setting SMTP_* and enabling SMTP_AUTH=true, SMTP_STARTTLS=true.

Database Schema & Sample Data
On first run, Spring auto-creates the table and inserts sample orgs.

src/main/resources/schema.sql

sql

CREATE TABLE IF NOT EXISTS organization (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);
src/main/resources/data.sql

sql

INSERT INTO organization (name, description, enabled) VALUES
  ('acme', 'Acme Corporation', true),
  ('globex', 'Globex Corp', true)
ON CONFLICT (name) DO NOTHING;
ERD
mermaid

erDiagram
  ORGANIZATION {
    bigint id PK
    varchar name "unique, not null"  // == Keycloak realm
    text description
    boolean enabled
  }
Users are in Keycloak, not in our DB.

Smoke Test (PowerShell, using curl)
Get token (see Security section).

Create org (ensures realm):

powershell

curl.exe -X POST "http://localhost:8080/api/organizations" ^
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" ^
  -d "{\"name\":\"serianu\",\"description\":\"Security Company\",\"enabled\":true}"
Create user (watch Mailhog for email):

powershell

curl.exe -X POST "http://localhost:8080/api/realms/serianu/users" ^
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" ^
  -d "{\"username\":\"josphat.muteti\",\"email\":\"josphat@example.com\",\"firstName\":\"Josphat\",\"lastName\":\"Muteti\",\"temporaryPassword\":\"Temp#1234\"}"
List users:

powershell

curl.exe -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/realms/serianu/users"
Disable / Enable / Delete user (check Mailhog after each):

powershell

curl.exe -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/realms/serianu/users/<USER_ID>/disable"
curl.exe -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/realms/serianu/users/<USER_ID>/enable"
curl.exe -X DELETE -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/realms/serianu/users/<USER_ID>"
Docker Services
postgres (port 5432)

keycloak (port 8081 on host)

smtp Mailhog (ports 1025/8025)

app Spring Boot API (port 8080)