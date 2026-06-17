# Contributing

## Getting started

Set up locally first — see [GETTING_STARTED.md](GETTING_STARTED.md).

## Workflow

1. Fork the repo and create a branch: `git checkout -b feat/your-feature`
2. Make changes following the patterns in [CODING_STANDARDS.md](CODING_STANDARDS.md)
3. Add tests — unit tests for service logic, integration tests for repository/API layers
4. Run the full test suite: `mvn verify`
5. Push and open a PR against `main`

## What belongs where

| Change | Module |
|--------|--------|
| New chat/AI capability | `ai-orchestrator-service` |
| New document format support | `ingestion-service` (Tika handles parsing automatically) |
| Auth, routing, rate limiting | `gateway-service` |
| Shared DTOs, exceptions, filters | `common-library` |

## Adding a new REST endpoint

Follow the checklist in `.claude/skills/add-endpoint.md` — it covers the full flow from DTO to controller to gateway route to tests.

## Code rules (short version)

- No business logic in controllers — controllers call services, nothing else
- Constructor injection only — no `@Autowired` on fields
- Never expose JPA entities in API responses — use records as DTOs
- All errors go through `GlobalExceptionHandler` in common-library
- Log at DEBUG on entry/exit of service methods, ERROR with context on exceptions
- Test naming: `methodName_givenCondition_expectedResult`

## Database migrations

- Schema lives in `src/main/resources/db/migration/`
- Add `V{n+1}__description.sql` — never edit existing migration files
- Flyway runs automatically on service startup

## PR checklist

- [ ] `mvn verify` passes locally
- [ ] New code has unit tests
- [ ] No hardcoded secrets or API keys
- [ ] `GETTING_STARTED.md` updated if setup steps changed
- [ ] `ARCHITECTURE.md` updated if a new service or data flow was added

## Questions

Open a [GitHub issue](../../issues) — label it `question`.
