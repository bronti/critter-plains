---
name: review-critter-plains
description: Review code changes against Critter Plains-specific correctness, architecture-boundary, and performance criteria — the project's known trouble spots (tick-loop mutation safety, Memory fog-of-war perf, :critters/:visualization boundary, Kotlin idioms used in this repo). Use when asked to review this project's code, or for /review-critter-plains.
---

Review the current diff (or the files/area the user points at) for the following. This checklist is specific to Critter Plains — it complements, not replaces, a general `/code-review`.

## Simulation correctness

- Critters iterating over a mutable collection during `WorldState.tick()` — check for `ConcurrentModificationException` risk (currently guarded by a `.toList()` copy; verify it stays that way).
- `Memory.positionsInRadius()` can produce positions outside map bounds — verify callers clamp or handle out-of-bounds/`VOID` gracefully.
- `act()` in `Actor.kt` — verify every `Intention` variant is handled; `COMMUNICATE` and `PROCREATE` are declared but not implemented.

## Module boundary violations

- `:visualization` must not import anything outside `critters.*` from the `:critters` module API; no OPENRNDR types may leak into `:critters`.
- `SimulationController` is the translation point between `Vector2` and `Position` — flag any direct use of `Vector2` inside `:critters`.

## State management

- `WorldState.observable()` / `WorldState.interactive()` are meant to be used within one tick — flag code that holds these references across ticks.
- `MutableGameMap` is shallow-cloned via `grid.clone()` (copies the array of references, not a deep copy) — flag mutations that could affect both copies.

## Kotlin idioms

- Prefer `data class` / `data object` over plain `class` / `object` for value types in `:critters`.
- Avoid `!!` (non-null assertion) — prefer safe calls, `requireNotNull`, or `checkNotNull` with a descriptive message.
- `require` / `check` calls should carry a descriptive message, not an empty string.
- Sealed hierarchies (`Territory`, `Terrain`) should be exhaustive in `when` expressions — flag any `when` missing a branch or `else`.

## Performance

- `Memory.closestFiltered()` iterates the entire territory map every call — flag if it's called in the hot tick path without caching.
- `Memory.positionsInRadius()` allocates a new list every tick per critter (O(critters × radius²)) — note if critter count or sight radius grows.

## Testing gaps

- Flag non-trivial pure logic in `:critters` with no test coverage, especially anything hard to verify visually (`Memory.update`, `act()`, `MapGeneration`).

Report findings the same way `/code-review` does — most-severe first, with file:line and a concrete failure scenario for each.
