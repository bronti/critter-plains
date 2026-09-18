---
title: 'Live Selection Tracking, Deselection & Auto-Clear on Death'
type: 'feature'
created: '2026-09-18'
status: 'done'
route: 'dispatch'
review_loop_iteration: 1
context: ['{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md']
baseline_commit: '8350c6a1c8c90f923b7e185fd71b3f0374f1f737'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `SimulationController.selectedCritter` is a one-time snapshot captured only at click time (`selectAt`) — `hunger` never updates after selection, and a selected critter that dies or is removed stays displayed as if still alive. Investigation also found Escape-to-deselect does not exist anywhere in the codebase, despite the epic listing it as already implemented — only click-to-select and click-elsewhere/other-critter work today.

**Approach:** Add one narrow, read-only `critter(name)` lookup to `:critters` (per epic Technical Decision) and call it once per tick in `SimulationController.update()`, right after the world tick, to re-resolve `selectedCritter` by its stored `CritterName`. This is the single liveness checkpoint; if the lookup returns null the selection clears. Add the missing `KEY_ESCAPE` handling in `InputHandler`, mirroring the existing `KEY_SPACEBAR` pattern exactly (`KEY_ESCAPE` is a named OPENRNDR constant, verified against the installed jar).

## Boundaries & Constraints

**Always:** The liveness re-resolve happens in exactly one place — `SimulationController.update()`, right after `state = game.stateView()` — never duplicated elsewhere. The `:critters` lookup is read-only and additive, no new mutation surface, no dependency from `:critters` on `:visualization`. `selectedCritter` stays exposed as `CritterStateView?` (unchanged consumer contract for `InfoPanel`) — only its re-resolution timing changes. Escape deselects unconditionally. Click-to-select and click-elsewhere/other-critter reselect must keep working exactly as today — add regression tests since none exist.

**Never:** Do not implement memory highlight, selection indicator, recenter, or speed tiers — Stories 1.4/1.5/1.6/1.7. Do not add a name→Critter index to `MutablePlacement` — a linear scan over the small population is fine. Do not change `selectedCritter`'s public type to `CritterName?`. Do not touch `MutablePlacement.rip()/move()/put()`, `Actor.kt`, or `MapGeneration.kt`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Select then critter starves | click critter, hunger reaches 40 next tick | `selectedCritter` becomes null the same tick `rip()` removes it | N/A |
| Select then hunger changes | click critter, ticks pass, hunger rises but critter alive | `InfoPanel` shows current hunger each tick, not the click-time value | N/A |
| Press Escape with a selection | `KEY_ESCAPE` keyDown, `selectedCritter != null` | selection clears to null | N/A |
| Press Escape with no selection | `KEY_ESCAPE` keyDown, `selectedCritter == null` | no-op | N/A |
| Click empty tile | click tile with no occupant | selection clears (existing behavior — add regression test) | N/A |
| Click a different critter | click critter B while critter A selected | selection switches to B (existing behavior — add regression test) | N/A |

</frozen-after-approval>

## Code Map

- `critters/src/main/kotlin/world/World.kt` (`WorldState`, line 31 `fun occupant(pos) = placement.occupant(pos)`) -- add `fun critter(name: CritterName): Critter? = placement.values.find { it.name == name }` right after `occupant`; linear scan, no new index (`placement.values` already used in `stats()`, line 34)
- `critters/src/main/kotlin/StateView.kt` (`interface GameStateView`, lines 10-13) -- add `fun critter(name: CritterName): CritterStateView?` to the interface
- `critters/src/main/kotlin/Game.kt` (`stateView()`, lines 40-43) -- implement the new interface member: `override fun critter(name: CritterName) = world.critter(name)?.view()`
- `visualization/src/main/kotlin/simulation/SimulationController.kt` (`selectedCritter` field line 35-36, `selectAt` lines 52-54, `update()` lines 60-67) -- in `update()`, after `state = game.stateView()` (line 63), add `selectedCritter = selectedCritter?.let { state.critter(it.name) }`; add a one-line `fun deselect() { selectedCritter = null }` delegate near `selectAt`; leave `selectAt` itself untouched
- `visualization/src/main/kotlin/input/InputHandler.kt` (`keyDown.listen` `when` block, lines 80-83, currently only `KEY_SPACEBAR -> controller.togglePause()`) -- add `KEY_ESCAPE -> controller.deselect()` as a second branch; import `org.openrndr.KEY_ESCAPE` alongside the existing `KEY_SPACEBAR` import (line 3)
- `visualization/src/test/kotlin/simulation/SimulationControllerTest.kt` (new file, no existing selection tests anywhere in the repo) -- cover: selection re-resolves live hunger change across ticks, selection auto-clears when the selected critter dies/is removed, `deselect()` clears selection, click-elsewhere/reselect via `selectAt`
- `visualization/src/test/kotlin/input/InputHandlerTest.kt` (existing, pure-function style only — no `SimulationController` instance) -- no change needed; Escape wiring is a one-line delegate with no extractable pure logic, covered instead via `SimulationControllerTest.deselect()`
- `visualization/src/main/kotlin/ai/Chronicler.kt` (constructor lines 50-53, `apiKey`/`enabled` derived from env/`.env` at lines 54-55) -- add the smallest seam that lets a test force `enabled = false` regardless of local `.env`/environment state, e.g. an internal constructor parameter defaulted to preserve today's env-reading behavior for all existing call sites
- `visualization/src/main/kotlin/simulation/SimulationController.kt` (constructor lines 11-18, `private val chronicler = Chronicler()` line 38) -- change to a constructor parameter defaulted to today's real `Chronicler()` (e.g. `chronicler: Chronicler = Chronicler()`), so `Visualization.kt`'s existing call site needs no change, but `SimulationControllerTest` can pass a disabled one

## Tasks & Acceptance

**Execution:**
- [x] `critters/src/main/kotlin/world/World.kt` -- add `WorldState.critter(name: CritterName): Critter?` -- gives `:visualization` a read-only way to re-resolve identity
- [x] `critters/src/main/kotlin/StateView.kt` + `Game.kt` -- expose `critter(name): CritterStateView?` through `GameStateView` -- keeps `:visualization` decoupled from raw `Critter`/`WorldState`
- [x] `visualization/src/main/kotlin/simulation/SimulationController.kt` -- re-resolve `selectedCritter` by name every `update()` tick; add `deselect()` -- the single liveness checkpoint the epic requires
- [x] `visualization/src/main/kotlin/input/InputHandler.kt` -- wire `KEY_ESCAPE` to `controller.deselect()` -- this binding doesn't exist yet despite the epic assuming it does
- [x] `visualization/src/test/kotlin/simulation/SimulationControllerTest.kt` -- new tests: live re-resolve, auto-clear on death, `deselect()`, click-elsewhere/reselect regression -- no selection tests exist today
- [x] `visualization/src/main/kotlin/ai/Chronicler.kt` + `visualization/src/main/kotlin/simulation/SimulationController.kt` -- add a disableable-`Chronicler` seam and default-parameterize it on `SimulationController` -- so tests never depend on `ANTHROPIC_API_KEY`/network state, per Spec Change Log
- [x] `visualization/src/test/kotlin/simulation/SimulationControllerTest.kt`'s death test -- construct its `SimulationController` with a disabled `Chronicler` -- prevents the up-to-3000-tick loop from making real Anthropic API calls

**Acceptance Criteria:**
- Given a critter is selected, when several ticks pass and its hunger changes, then the displayed `CritterStateView` reflects the current hunger, not the click-time value.
- Given a selected critter dies or is removed from the world, when the next tick completes, then `selectedCritter` is null.
- Given a critter is selected, when Escape is pressed, then selection clears; when Escape is pressed with nothing selected, nothing happens.
- Given a critter is selected, when the player clicks an empty tile or a different critter, then selection clears or switches respectively.

## Implementation Notes

- `SimulationControllerTest` builds a real `Game` (11x11 map, the minimum `MutableGameMap` allows) rather than mocking, since `Game`/`WorldState` have no injection seam. The death test relies on food being finite (blobs are placed once at `Game` init and never regenerate), which bounds the sole critter's worst-case lifespan; it loops with a generous 3000-tick cap and asserts termination before the cap rather than asserting an exact tick count.
- Pre-existing, unrelated: `critters:test` currently fails on `CritterTest.kt:74` ("intent is EAT when hunger is above ten, EXPLORE otherwise") on the baseline commit too (verified via `git stash` + rerun) — `Critter.intent()`'s thresholds changed in later commits (49832ac, 51567d8) without updating that test. Not touched here since it's out of this story's scope.
- Re-implementation (review loop 1): added `Chronicler(forceDisabled: Boolean = false)` and a defaulted `chronicler` constructor parameter on `SimulationController` per the Spec Change Log; every test now passes `Chronicler(forceDisabled = true)`, so the suite makes zero real Anthropic API calls regardless of local `.env`. Replaced bare `!!` with `checkNotNull(...) { "message" }` on all nullable `critter(name)` lookups in the test file per the Review Triage Log patch item. Added a comment on `WorldState.critter()` documenting the intentional O(n) scan and its exclusion from `World.Observable`. Independently re-verified: `.\scripts\compile-check.ps1` → `COMPILE OK`; `.\gradlew.bat :visualization:test --tests "simulation.SimulationControllerTest" --rerun` → `BUILD SUCCESSFUL`, all 8 tests pass.
- Matrix Test Audit note: the two Escape-key matrix rows (with/without an active selection) are not covered by an automated test that exercises `InputHandler`'s actual `keyDown` dispatch — only `SimulationController.deselect()` itself is unit-tested. This is the same, already-triaged **defer** from review loop 0 (matches the pre-existing, identically-untested `KEY_SPACEBAR` binding); not re-litigated here.

## Spec Change Log

- **Triggered by:** review finding "death test fires real Anthropic API calls" (bad_spec, see Review Triage Log). **Amended:** Code Map and Tasks below now require a substitutable/disableable `Chronicler` and its use in the death test, instead of the real network-backed one. **Avoids:** every run of `SimulationControllerTest`'s death test making real, paid Anthropic API calls whenever a developer's environment has `ANTHROPIC_API_KEY` set (this repo's `.env` does). **KEEP** (verified correct in the reverted implementation, must survive re-derivation unchanged): `WorldState.critter(name)` linear scan exactly as it was (`placement.values.find { it.name == name }`); the `GameStateView.critter(name)` interface addition and its `Game.stateView()` override; `SimulationController.update()`'s single re-resolve line `selectedCritter = selectedCritter?.let { state.critter(it.name) }` placed immediately after `state = game.stateView()`; `SimulationController.deselect()` and `InputHandler`'s `KEY_ESCAPE -> controller.deselect()` wiring; all seven non-death test cases and their assertions in `SimulationControllerTest.kt` (selecting the only critter, hunger re-resolving every tick, hunger mirroring ground truth across ticks, `deselect()` clearing an active selection, `deselect()` no-op, clicking an empty tile, clicking a different critter) — only the death test's `Chronicler` needs to change, not its tick-loop/termination logic.

## Review Triage Log

- **false** — blind-hunter claimed `docs/epics/epic-1-visualization-controls/sprint-status.yaml` still lists this story as `backlog`, disagreeing with the story file. Verified: that file does not exist anywhere in the repo (confirmed by full-repo search); the real tracker, `_bmad-output/implementation-artifacts/sprint-status.yaml`, currently reads `in-progress` for this story, not `backlog`. → **rejected** (false).
- **medium** — blind-hunter and verification-gap (filed pre-verified) both confirmed no test exercises the new `KEY_ESCAPE -> controller.deselect()` branch in `InputHandler.kt`'s `keyDown.listen` block — only the pre-existing, identically-untested `KEY_SPACEBAR -> controller.togglePause()` binding has the same shape. Closing it needs extending the file's existing pure-function-extraction pattern (`panDelta`/`zoomFactorForKey`) to key dispatch itself, a refactor larger than this diff and one that would also touch the pre-existing Spacebar binding. → **defer**.
- **low** — edge-case-hunter found two unguarded `!!` on the nullable `GameStateView.critter(name)` lookup in the new test file (`selectByName` helper, and the "clicking a different critter switches the selection" test), backed only by documented-as-astronomically-unlikely collision odds rather than a guard. → **patch**: swap for `checkNotNull(...) { "message" }` for a clear failure instead of a bare NPE.
- **low** — blind-hunter: `WorldState.critter()`'s intentional O(n) linear scan (explicitly required by this spec's frozen Boundaries) and its deliberate exclusion from `World.Observable` have no comment explaining either is intentional, inviting a future "fix" of a non-bug. → **patch**: one-line comment on `WorldState.critter()`.
- **defer** — blind-hunter: `AGENTS.md`'s `InputHandler.kt` key-binding description doesn't mention the new Escape/deselect binding. Fix touches an agent-context file (`AGENTS.md`) — per rule, routes to **defer** regardless of severity; this is exactly what `wrap-session` reconciles at session close, not per-story.
- **low, bad_spec** — blind-hunter flagged the new death test's up-to-3000-tick loop through `controller.update()` as an unacknowledged side channel to `Chronicler.maybeUpdate()` (fires every 100 ticks or on a population drop). Verified as live, not hypothetical: this repo has a `.env` with `ANTHROPIC_API_KEY` configured, and `Chronicler.enabled` derives from it with no override hook; `SimulationController` constructs its own private `Chronicler()` with no injection seam. So every run of this test fires at least one real Anthropic API call (guaranteed on the death-triggered population drop), with no fix confined to the test file — the smallest correct fix (a substitutable/disableable `Chronicler`) adds public surface, so it isn't patch-eligible, and the spec never anticipated this interaction. → **bad_spec**: Code Map/Tasks amended below to require a disableable `Chronicler` and its use in this test.
- **defer** — blind-hunter and verification-gap both note `critters:test`'s pre-existing `CritterTest.kt:74` failure is untouched. Confirmed pre-existing on the baseline commit (`git stash` + rerun, logged in Implementation Notes) — unrelated to this story. → **defer**.
- **low** — blind-hunter and verification-gap: the new test's comment says hunger is "floored at 40," but `Critter.tick()`'s `min(40, hunger + 1)` is a cap/ceiling, not a floor. Cosmetic wording only. → **patch**: reword to "capped." (Superseded — review loop 1 rewrote this test file without the phrase; no longer applicable.)

### Review loop 1 (post re-implementation)

- **medium — carried** — blind-hunter and verification-gap both re-confirmed no test exercises `InputHandler`'s `KEY_ESCAPE -> controller.deselect()` dispatch. Same location and claim as loop 0's row; code still reads as that row describes. → **defer** (carried; appended to `deferred-work.md`, not re-patched).
- **defer** — blind-hunter re-confirmed `AGENTS.md`'s `InputHandler.kt`/`SimulationController` entries don't mention Escape/`deselect()`/`critter(name)`. Fix touches an agent-context file → **defer** (appended to `deferred-work.md`).
- **medium** — verification-gap and blind-hunter both independently found the new `Chronicler(forceDisabled: Boolean)` seam has no direct test: nothing asserts `latestChronicle`/`enabled` reflects `forceDisabled = true`, or that `maybeUpdate` no-ops under it. Verified real: a regression in the `!forceDisabled && !apiKey.isNullOrBlank()` precedence would not fail any existing test — it would only surface as unexpected live API calls on a machine with `ANTHROPIC_API_KEY` set, exactly the scenario this seam exists to prevent. → **patch**: add a direct `Chronicler(forceDisabled = true)` test asserting the disabled-fallback `latestChronicle` text.
- **low** — blind-hunter: `InfoPanel.kt:25`'s `// todo: critter death` comment is now stale — this story's auto-clear-on-death (`selectedCritter` becomes null the same tick the critter dies) is exactly what it was waiting on. Verified: comment still present, diff's fix resolves it. → **patch**: delete the comment.
- **low** — blind-hunter: `WorldState.critter()`'s comment (added in loop 0) documents the intentional O(n) scan but not that this diff changes it from a one-off, click-triggered lookup into a per-tick call whenever a critter is selected, nor that it's a deliberate identity-based lookup bypassing fog-of-war/visibility filtering (unlike `Memory`-based queries). Both true and worth a short addition for a future reader/perf-profiler. → **patch**: extend the existing comment.
- **low, rejected** — blind-hunter claimed the "clicking a different critter" test has a ~1.6% flake risk from random position collision. Verified the actual math: 1.6% is the probability of *any* pairwise collision among 3 draws on 121 tiles, but the test only fails if all 3 collapse to a single position (`names.size < 2`) — a pairwise collision alone still leaves 2 distinct critters, which the test's `check(names.size >= 2)` accepts. Real failure probability is `(1/121)^2 ≈ 0.0068%`. At that rate, for a solo-hobby project's occasional local test runs, unlikely to ever be met — and the only real fix (an injectable/seeded critter-placement seam) adds new public surface, which isn't a trivial correction. → **rejected**.
- **low, rejected** — blind-hunter claimed `Chronicler`'s disabled-fallback message is ambiguous between "no API key" and "`forceDisabled`" causes, and that `forceDisabled` "isn't private." Verified: `forceDisabled` is a plain constructor parameter with no `val`/`var` — it isn't stored as a property at all, so it cannot be read or leaked anywhere after construction; the claim of it being "not private" doesn't hold. The message-ambiguity scenario would only matter if `forceDisabled = true` were ever used outside tests, which nothing in this diff does. → **rejected**.

## Verification

**Commands:**
- `.\scripts\compile-check.ps1` -- expected: no compile errors.
- `.\scripts\build-quiet.ps1 test` -- expected: `BUILD SUCCESSFUL`, new `SimulationControllerTest` passing.

**Manual checks (if no CLI):**
- Run the app, select a critter, wait several ticks, confirm hunger updates live in `InfoPanel`; let it starve and confirm selection clears; press Escape to deselect.
