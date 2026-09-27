# Project instructions

## Architecture
- Controllers in `src/main/java/.../controller` — thin, no business logic.
- Business logic in `src/main/java/.../service`.
- Repositories in `src/main/java/.../repository`, extending Spring Data interfaces.
- Use DTOs for request/response; never expose JPA entities directly in controllers.

## Conventions
- Constructor injection only — no field injection with `@Autowired`.
- Use `Optional<T>` for repository lookups that may not find a result.

## Avoid
- Don't use `@Autowired` on fields.
- Don't add logic to controllers — delegate to services.