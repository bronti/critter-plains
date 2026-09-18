# Epic 1 Context: Visualization & Interaction Controls

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Turn the Critter Plains viewer from a fixed-scale, single-tap display into a proper observation tool: the builder can pan continuously and zoom between overview and detail, select and track any critter (including its fog-of-war memory, a recenter shortcut, and an always-visible indicator), and control simulation pace via pause and three speed tiers — all from the running viewer, with no code edits and no changes to critter AI or behavior. This is a solo-hobby-project epic with no quantitative success bar beyond the builder finding sessions pleasant to use.

## Stories

- Story 1.1: Camera & Hold-to-Pan
- Story 1.2: Zoom In/Out
- Story 1.3: Live Selection Tracking, Deselection & Auto-Clear on Death
- Story 1.4: Memory (Fog-of-War) Highlight
- Story 1.5: Recenter Viewport on Selected Critter
- Story 1.6: Selection Indicator Arrow
- Story 1.7: Speed Tiers
- Story 1.8: Pause & Speed Tier Status Badges

## Requirements & Constraints

- Panning must be continuous while a direction key is held (`WASD` or arrows), stop cleanly at the map boundary with no overshoot/wraparound, and support diagonal movement when two perpendicular keys are held together.
- Zoom must work via scroll wheel and via a dedicated key pair, be bounded (can't zoom out past the full map or in past an unreasonably large tile), and must not fire when the cursor/scroll is over the info panel rather than the map viewport.
- Critter selection (click to select, click elsewhere/other critter/`Escape` to deselect) is already implemented and is locked in as a regression-covered acceptance criterion, not just preserved incidentally.
- Selected-critter state must stay live: it re-resolves every tick (not just at click time) and auto-clears if the critter dies or is removed — no stale/frozen-snapshot display.
- Memory highlight: unremembered tiles are tinted while a critter is selected; remembered tiles render unchanged; tint updates live as memory grows and clears immediately on deselect.
- Recenter (`C`) snaps the viewport to the selected critter, clamped at map edges the same way as ordinary panning; no-ops when nothing is selected.
- A selection indicator arrow is always visible while a critter is selected — pointing down at it when on-screen, pointing toward it from the nearest viewport edge when off-screen — and disappears immediately on deselect.
- Pause/resume via spacebar is already implemented and locked in as a regression-covered acceptance criterion.
- Three Speed Tiers (keys `1`/`2`/`3`, multipliers 0.5x/1x/2x of the default tick rate) must be switchable at any time, including while paused (switching tiers never auto-resumes); Tier 2 (1x) is the startup default with no key press required.
- A "PAUSED" badge and a speed-tier badge must always be visible on screen, updating immediately on state change.
- Panning and zooming must not visibly drop frames at the existing render rate during continuous input.
- Explicitly out of scope: any change to critter AI/behavior, mouse-drag panning, multi-critter selection, cross-session persistence of camera/selection, touch/gamepad/remappable-keybinding support, minimap, animated camera transitions, camera bookmarks, keybinding configuration UI.

## Technical Decisions

- A new `Camera` (package `simulation`) becomes the sole owner of viewport position and zoom scale; `SimulationController` composes it instead of holding viewport fields itself. Every world↔screen conversion anywhere in `:visualization` goes through this one `Camera`. Its screen-space convention is the map-viewport rectangle only (origin `0,0` to `viewportWidthPx × viewportHeightPx`) — it has no knowledge of the info panel, so callers must confirm a raw window/mouse coordinate falls inside that rectangle before passing it in. Zoom exposes one entry point, `zoomAt(anchorScreenPos, factor)` — scroll wheel calls it anchored at the cursor, key-based zoom (`+`/`-`, single discrete step per press, not held-continuous) anchored at the viewport center. The off-screen render buffer keeps fixed pixel dimensions regardless of zoom; each frame redraws tiles at the live scale into that same buffer rather than scaling a stale blit.
- A new `OverlayView` (package `view`) renders memory tint, the selection indicator, and the pause/tier badges as a screen-space pass drawn *after* the world buffer is blitted, in a fixed order: tint → indicator → badges (lowest to topmost). The world-buffer renderer keeps rendering only sim ground truth and stays unaware of selection/UI state, with one narrow exception: it may read `Camera`'s live zoom scalar each frame to size tile draws (world geometry, not UI state). Each `OverlayView` sub-pass resets its own `Drawer` fill/stroke/strokeWeight state rather than relying on whatever ran before it — this hygiene pattern applies to every sub-pass added across the epic.
- A `SpeedTier` enum (`SLOW=0.5`, `NORMAL=1.0`, `FAST=2.0`) is first-class state on `SimulationController`; tick interval is always derived from it (`baseInterval / tier.multiplier`), never the reverse, sidestepping integer-fps rounding. The tier↔display-label mapping lives once on `SpeedTier` itself — no duplicated mapping in the badge rendering.
- Continuous input (hold-to-pan) is polled once per frame via OPENRNDR's built-in `KeyTracker.pressedKeys` (keyed by key name, not the `Int` codes used by existing discrete handlers) — not hand-rolled key-state tracking and not GLFW key-repeat. All other actions (select, deselect, pause, tier switch, recenter, scroll zoom, and key-based zoom) stay discrete/event-driven; key-based zoom is deliberately excluded from the held-key poll set to avoid double-firing.
- `:critters` gains exactly one narrow, additive, read-only lookup — an identity-keyed `critter(name)` accessor — as the epic's sole, scoped exception to the "no outgoing dependency on `:visualization`" rule; no new mutation surface. `SimulationController` calls it right after every tick to re-resolve the selection, and this is the *only* place liveness/staleness is checked — every other consumer (overlay tint, indicator, recenter, info panel) is a pure reader of the already-resolved selection state.

## UX & Interaction Patterns

- Key bindings: `WASD`/arrow keys = hold-to-pan (continuous); scroll wheel = zoom anchored at cursor; `+`/`-` = zoom anchored at viewport center (one discrete step per press); click = select/switch selection; click empty tile or `Escape` = deselect; `C` = recenter on selection; `1`/`2`/`3` = speed tier; spacebar = pause/resume.
- Memory tint applies only to tiles outside the selected critter's remembered area; remembered tiles are visually identical to the unselected state.
- The selection indicator is an arrow: pointing straight down at the critter when it's on-screen, or positioned at the nearest viewport edge pointing toward the critter's true position when it's off-screen.
- Status badges: a "PAUSED" badge shown only while paused, and a persistent speed-tier badge (`1`/`2`/`3`) — both always rendered topmost, updating immediately on state change.
- No animated/tweened transitions anywhere — pan, zoom, and recenter are instantaneous per input.

## Cross-Story Dependencies

- Story 1.1 (`Camera`) is a prerequisite for 1.2 (zoom uses the same `Camera`), 1.5 (recenter uses `Camera`'s clamped positioning), and 1.6 (indicator projection uses `Camera`'s screen-space math).
- Story 1.3 (live-resolved, auto-clearing selection) is a prerequisite for 1.4, 1.5, and 1.6 — all three are pure readers of the selection state it maintains, including the dead-critter auto-clear behavior.
- Story 1.4 introduces `OverlayView`; Story 1.6 adds its second sub-pass (indicator) and Story 1.8 its third (badges), all sharing the fixed tint → indicator → badges draw order and per-sub-pass `Drawer`-reset convention established in 1.4.
- Story 1.7 (`SpeedTier` state) is a prerequisite for 1.8 (tier badge reads `SpeedTier`'s own display-label property).
