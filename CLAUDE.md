# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## Project: google-login-api (Backend)

**Stack:** Spring Boot 3.3.4 / Java 21, port 8080  
**Frontend:** Serves `google-login` at port 3000

### Structure
```
src/main/java/com/example/googlelogin/
├── controller/AuthController.java   # POST /api/auth/{check-email,login,logout}, GET /api/auth/me
├── service/AuthService.java         # SHA-256 password verification
├── service/SessionStore.java        # In-memory session (SecureRandom ID)
├── model/AuthRequest.java           # toString() masks password
└── config/CorsConfig.java           # Allows localhost:3000 only
```

### Dev
```bash
mvn spring-boot:run   # http://localhost:8080
```

### Key constraints
- **Sessions are in-memory** — all sessions are lost on restart; incompatible with multi-pod deployments without Redis.
- **Passwords use SHA-256 without salt** — vulnerable to rainbow tables; use bcrypt/Argon2 for production.
- **Users are hardcoded** — no database; cannot add users dynamically.
- **CORS** is restricted to `localhost:3000` — update `CorsConfig.java` when deploying.
- `Cookie: Secure; HttpOnly; SameSite=Lax` — local HTTP testing will not send the Secure cookie.
