# Skill: add-endpoint

Add a new REST endpoint to one of the services following project conventions.

## Usage
`/add-endpoint <service> <HTTP-method> <path> <description>`

Example: `/add-endpoint ai-orchestrator POST /api/chat/history Get conversation history for a session`

## Checklist

### 1. Model / DTO  
- Create a record in `.../model/` — immutable, no business logic.
- Use `@NotBlank`, `@NotNull` for required fields.

### 2. Service interface  
- Add method signature to the `*Service` interface in `.../service/`.

### 3. Service implementation  
- Implement in `Default*Service`.
- Constructor injection only (no `@Autowired`).
- Log entry/exit at DEBUG, errors at ERROR with structured fields.

### 4. Controller  
- Add `@PostMapping` / `@GetMapping` etc. to the controller.
- Controllers call services — no business logic.
- Return the DTO, not the entity.
- Annotate request body with `@Valid`.

### 5. Gateway route (if new path prefix)  
- Add route in `gateway-service/src/main/resources/application.yml` under `spring.cloud.gateway.routes`.

### 6. Tests  
- Unit test in `*ControllerTest` — mock the service, verify HTTP status and response body.
- Unit test in `Default*ServiceTest` — mock repos/external clients, verify logic.

### 7. Naming convention
- Test method: `methodName_givenCondition_expectedResult`
- REST path: `/api/<resource>` (lowercase, kebab-case)
