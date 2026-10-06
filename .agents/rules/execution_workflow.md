---
trigger: always_on
description: Mandatory execution rules for maximum speed, targeted testing, and zero-waste turnaround
---

# Koto Fast Execution & Workflow Directives

To guarantee maximum speed and eliminate wasted time, the AI must strictly obey these execution rules:

---

## 1. Zero Full-Suite Testing (Strict Prohibition)
- **NEVER** run the full project test suite (e.g., `./gradlew testDebugUnitTest` without filters).
- If a test is needed, test **ONLY** the single class or method touched (e.g., `--tests "com.koto.app.SpreadsheetEngineTest"`).
- For non-code tasks (documentation, notes, design philosophy, git tags, pure UI styling, asset additions), **DO NOT RUN TESTS AT ALL**.

## 2. Direct-Action First (No Over-Analysis)
- Execute the exact task requested on the exact file immediately.
- Do not read dozens of unrelated files or perform exploratory codebase sweeps when the target file or task is already clear.
- Keep tool calls targeted and minimal.

## 3. No Rabbit Holes / Unrelated Bug Chasing
- If an unrelated test or background check fails, **DO NOT** investigate or fix it unless the user explicitly requested it.
- Deliver the requested work first. Never derail a simple task into a multi-step debugging detour.

## 4. Immediate Delivery & Concise Updates
- Edit the file, verify only what was changed, and report back immediately.
- Do not post lengthy essays or run background timers when a synchronous, fast action completes the job in seconds.
