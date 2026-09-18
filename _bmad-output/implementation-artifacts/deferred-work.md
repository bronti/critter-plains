# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/1-2-zoom-in-out.md`
  summary: `BufferedWorldView.render()`'s fractional-viewport tile-position math has no direct test; a sign flip or off-by-one wouldn't be caught by the current suite.
  evidence: Verification-gap review confirmed no test constructs or exercises `BufferedWorldView`. **Update (2026-09-18):** the predicted risk materialized — the original `col * cellSize` then `/ cellSize` round-trip through `Camera.mapPosition()` lost precision at some zoom levels, causing duplicated/skipped tile columns in the running app. Fixed by computing the world `Position` directly from loop indices instead of round-tripping through `Vector2`/`cellSize` division (commit `67d62f6`). The tile-iteration math is still inline in the OPENRNDR-coupled `render()` and still has no direct unit test — extracting it out (mirroring `panDelta`/`zoomFactorForKey`) remains open.

- source_spec: `_bmad-output/implementation-artifacts/1-3-live-selection-tracking-deselection-auto-clear-on-death.md`
  summary: No test exercises `InputHandler`'s actual `KEY_ESCAPE -> controller.deselect()` dispatch — only `SimulationController.deselect()` itself is unit-tested. A regression that unwires or mistypes the key binding in `InputHandler.kt` would pass every test in the repo.
  evidence: Blind-hunter and verification-gap independently confirmed via repo-wide search: no test constructs an `InputHandler` or invokes `keyDown.listen`'s `when` block. This mirrors the pre-existing, identically-untested `KEY_SPACEBAR -> controller.togglePause()` binding. Closing it needs extending the file's existing pure-function-extraction pattern (`panDelta`/`zoomFactorForKey`) to key dispatch itself — a refactor larger than this story's scope, and one that would also touch the pre-existing Spacebar binding, not just Escape.

- source_spec: `_bmad-output/implementation-artifacts/1-3-live-selection-tracking-deselection-auto-clear-on-death.md`
  summary: `critters:test`'s pre-existing `CritterTest.kt:74` failure ("intent is EAT when hunger is above ten, EXPLORE otherwise") is untouched by this story.
  evidence: Confirmed pre-existing on the story's baseline commit via `git stash` + rerun (logged in the story's Implementation Notes) — `Critter.intent()`'s thresholds changed in later commits (49832ac, 51567d8) without updating that test. Unrelated to this story's scope.
