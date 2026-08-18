# Team Access Control API
 
A multi-tenant IAM-style backend and admin dashboard — authentication, organization management, role-based access control (RBAC), team invitations, session management, and audit logging.
 
Built as a full-stack portfolio project: **Spring Boot** (Java 21) backend + **React** (Vite, plain JS) frontend, fully containerized with **Docker Compose**.
 
---
 
## Features
 
- **Authentication** — signup/login with JWT access tokens, BCrypt password hashing
- **Multi-tenant organizations** — users can belong to multiple organizations, each with isolated data
- **Role-based access control** — Owner / Admin / Member / Viewer roles, each with an explicit, seeded permission set (deny-by-default)
- **Team invitations** — email-based invite flow with expiring tokens; invitee must accept before gaining access; last-owner protection prevents an org from ever being left ownerless
- **Session management** — view and revoke active sessions per user, Redis-backed
- **Audit logging** — every access-relevant event (logins, invites, role changes, member removal) is recorded and viewable per organization
- **Rate limiting** — Redis-backed (Bucket4j) on auth routes to slow down credential-stuffing attempts
- **Pagination** — Members, Sessions, and Audit Log lists are paginated server-side
---
 
## Tech stack
 
**Backend**
- Java 21, Spring Boot 3
- Spring Security + JWT (stateless auth)
- Spring Data JPA + PostgreSQL
- Flyway (database migrations)
- Redis (sessions, rate limiting)
- Spring Mail (Gmail SMTP — invitation emails)
- springdoc-openapi (Swagger UI at `/docs`)
- JUnit 5 + Mockito + AssertJ (unit tests)
**Frontend**
- React (Vite, plain JavaScript — no TypeScript)
- React Router
- Zustand (client state)
- Axios
**Infrastructure**
- Docker + Docker Compose (Postgres, Redis, backend, frontend all containerized)
- Nginx (serves the built frontend in production)
---
 
## Architecture
 
```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │─────▶│    Backend   │─────▶│  PostgreSQL │
│ React + Nginx│      │  Spring Boot │      │             │
└─────────────┘      └──────┬───────┘      └─────────────┘
                             │
                             ▼
                      ┌─────────────┐
                      │    Redis    │
                      │ (sessions,  │
                      │ rate limit) │
                      └─────────────┘
```
 
Each organization's data (members, roles, audit logs, invitations) is isolated per-tenant via a `Membership` join table — a user can belong to multiple organizations, but never sees data from an organization they're not a member of.
 
---
 
## Getting started
 
### Option A — Docker (recommended, matches production setup)
 
**Prerequisites:** Docker Desktop installed and running.
 
```bash
git clone https://github.com/YOUR_USERNAME/team-access-control.git
cd team-access-control-app
```
 
Create a `.env` file in the project root:
```
DB_PASSWORD=your-db-password
JWT_SECRET=a-long-random-secret-at-least-256-bits
MAIL_USERNAME=your-gmail-address@gmail.com
MAIL_PASSWORD=your-16-char-gmail-app-password
```
 
Then:
```bash
docker compose up --build
```
 
- Frontend: [http://localhost:5173](http://localhost:5173)
- Backend API: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/docs](http://localhost:8080/docs)
### Option B — Run locally without Docker
 
**Backend:**
```bash
cd team-access-control
# set DB_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_PASSWORD as environment variables,
# or run a local Postgres/Redis matching application.properties defaults
mvn spring-boot:run
```
 
**Frontend:**
```bash
cd client
npm install
npm run dev
```
 
---
 
## Running tests
 
```bash
cd team-access-control
mvn test
```
 
22 unit tests covering `AuthService`, `OrganizationService`, `RoleService`, `AuditLogService`, and `SessionController` (including an authorization test verifying users cannot revoke another user's session).
 
See `Team Access Control API - Testing Documentation.docx` for the full testing writeup (unit tests, manual Swagger/Postman verification, integration testing status).
 
---
 
## Key API endpoints
 
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/signup` | Create a new user account |
| POST | `/api/v1/auth/login` | Log in, returns JWT access + refresh token |
| POST | `/api/v1/organizations` | Create a new organization (creator becomes Owner) |
| GET | `/api/v1/organizations` | List organizations the current user belongs to |
| POST | `/api/v1/organizations/{orgId}/invite` | Invite a teammate by email + role |
| POST | `/api/v1/organizations/accept-invite` | Accept a pending invitation |
| GET | `/api/v1/organizations/{orgId}/members` | List organization members (paginated) |
| PUT | `/api/v1/organizations/{orgId}/members/{userId}/role` | Change a member's role |
| DELETE | `/api/v1/organizations/{orgId}/members/{userId}` | Remove a member |
| GET | `/api/v1/organizations/{orgId}/audit-logs` | View audit log (paginated) |
| GET | `/api/v1/sessions` | List active sessions (paginated) |
| DELETE | `/api/v1/sessions/{sessionId}` | Revoke a session |
| GET | `/api/v1/roles` | List roles and their permissions |
 
Full interactive API documentation available via Swagger UI at `/docs` once the backend is running.
 
---
 
## Known limitations / roadmap
 
- Invitation emails are sent via Gmail SMTP — fine for demo purposes, would move to a dedicated transactional email provider (SendGrid/SES) for real production use
- Integration tests (Testcontainers + full Spring context) are written but require a local Docker environment to run
- No automated E2E tests (Playwright/Cypress) yet
- No CI pipeline yet — tests are run manually via `mvn test`
---
 
## License
 
Personal portfolio project.
 
