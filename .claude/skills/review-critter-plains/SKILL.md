---
name: review-critter-plains
description: Review code changes against Critter Plains-specific correctness, architecture-boundary, and performance criteria — the project's known trouble spots (tick-loop mutation safety, Memory fog-of-war perf, :critters/:visualization boundary, Kotlin idioms used in this repo) — using a behavior-first, evidence-first review methodology. Use when asked to review this project's code, or for /review-critter-plains.
---

Review the current diff (or the files/area the user points at) the way a strong Kotlin teammate familiar with this codebase would: behavior first, risk first, evidence first. This complements, not replaces, a general `/code-review`.

## Read in this order

- The diff or changed files.
- Related tests (or the lack of them).
- Build/config changes (`build.gradle.kts`, `gradle.properties`).
- Impacted call sites — trace across the `:critters` / `:visualization` boundary, not just the changed file in isolation; many bugs here live in the seam between `WorldState`, `Actor`, `SimulationController`, and the views.

## Review dimensions

### Simulation correctness

- Critters iterating over a mutable collection during `WorldState.tick()` — check for `ConcurrentModificationException` risk (currently guarded by a `.toList()` copy; verify it stays that way).
- `Memory.positionsInRadius()` can produce positions outside map bounds — verify callers clamp or handle out-of-bounds/`VOID` gracefully.
- `act()` in `Actor.kt` — verify every `Intention` variant is handled; `COMMUNICATE` and `PROCREATE` are declared but not implemented.

### Module boundary violations

- `:visualization` must not import anything outside `critters.*` from the `:critters` module API; no OPENRNDR types may leak into `:critters`.
- `SimulationController` is the translation point between `Vector2` and `Position` — flag any direct use of `Vector2` inside `:critters`.

### State management

- `WorldState.observable()` / `WorldState.interactive()` are meant to be used within one tick — flag code that holds these references across ticks.
- `MutableGameMap` is shallow-cloned via `grid.clone()` (copies the array of references, not a deep copy) — flag mutations that could affect both copies.

### Async / API-call correctness

- Anything calling an external API (e.g. `Chronicler`'s Anthropic calls) must run off the render thread and never let a failure crash or block the tick/render loop — verify errors degrade to a fallback instead of propagating.
- Check throttling logic (e.g. `minTicksBetweenCalls`) still bounds call frequency after a change — an unthrottled path to a paid external API is a real cost/production risk, not a style nit.

### Kotlin idioms

- Prefer `data class` / `data object` over plain `class` / `object` for value types in `:critters`.
- Avoid `!!` (non-null assertion) and unsafe platform types — prefer safe calls, `requireNotNull`, or `checkNotNull` with a descriptive message.
- `require` / `check` calls should carry a descriptive message, not an empty string.
- Sealed hierarchies (`Territory`, `Terrain`) should be exhaustive in `when` expressions — flag any `when` missing a branch or `else`.
- Watch for `lateinit` misuse — a property that can legitimately stay uninitialized should be nullable instead.

### Performance

- `Memory.closestFiltered()` iterates the entire territory map every call — flag if it's called in the hot tick path without caching.
- `Memory.positionsInRadius()` allocates a new list every tick per critter (O(critters × radius²)) — note if critter count or sight radius grows.

### Testing gaps

- Flag non-trivial pure logic in `:critters` with no test coverage, especially anything hard to verify visually (`Memory.update`, `act()`, `MapGeneration`).
- Don't wave off a missing test just because the code "looks straightforward" — that's exactly the code that regresses silently later.

## What counts as a real finding

- A correctness bug.
- A production-risking design choice (e.g. an unthrottled external API call, a boundary violation that will bite the next refactor).
- A likely regression.
- A missing test for a meaningful failure path.

Minor style points are secondary and should never drown out a real risk.

## Review heuristics

- Prefer a small number of well-supported findings over a long list of weak suspicions.
- Tie every finding to behavior, not just taste.
- Before flagging something as wrong, check whether it matches an existing, intentional pattern elsewhere in this repo (e.g. the naive step-toward pathfinding, the deliberately unfixed map-gen seed) — those are known, accepted tradeoffs, not bugs.
- Distinguish `must fix` from `consider improving`.

## Guardrails

- Do not nitpick naming or formatting when the change contains a higher-severity risk.
- Do not invent risks without evidence in the code.
- Do not praise or summarize before surfacing findings.
- Do not apply generic advice that doesn't hold for this project's actual architecture (e.g. Spring/JPA-style review criteria — this project has no Spring dependency).

## Output

Findings first, most-severe first, with file:line and a concrete failure scenario for each — matching `/code-review`'s format. Follow with open questions/assumptions only where they'd change the review outcome, then a brief summary. If there's nothing material, say so explicitly and still note residual risk or testing gaps rather than staying silent.
