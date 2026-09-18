# critter-plains

A creature simulation.

## Structure

- **critters** — simulation logic (world, creatures, map, game loop)
- **visualization** — rendering via [OPENRNDR](https://openrndr.org/)
- **src** — entry point (`Main.kt`)

## Controls

- **`W`/`A`/`S`/`D` or arrow keys** — hold to pan the camera continuously; hold two perpendicular keys to pan diagonally. Panning clamps at the map edges.
- **Scroll wheel** — zoom in/out, anchored at the cursor.
- **`+`/`-`** — zoom in/out one step at a time, anchored at the viewport center.
- **Spacebar** — pause / resume the simulation.
- **Hover** over a critter — shows a tooltip with its name and current hunger.
- **Click** a critter — selects it; its name and hunger stay live in the right-side info panel as they change, alongside an AI-generated narration of the population's state (updates periodically; needs `ANTHROPIC_API_KEY`, otherwise falls back to a static line). While selected, tiles the critter hasn't explored yet are tinted on the map.
- **Click** an empty tile or another critter, or press **`Escape`** — deselects (or switches selection). Selection also clears automatically if the critter dies.

## Critter Behaviour

- **Hunger** starts randomly between 1 and 20, increases by 1 every tick, and the critter dies once it reaches 40.
- **Memory** is per-critter fog-of-war: each tick, a critter observes a 7-tile radius around itself and remembers the terrain and other critters it has seen there (a remembered critter's old location is dropped once it's seen somewhere else).
- **Intent** is decided each tick from hunger: `EAT` once hunger is above 10, otherwise `EXPLORE`. (`COMMUNICATE` and `PROCREATE` are modeled but not implemented yet.)
- **EXPLORE** — steps to a random traversable neighboring tile.
- **EAT** — if the critter remembers an unoccupied `FOOD` tile, it steps one tile at a time toward it until it arrives, then eats it (hunger drops by 10, floored at 0, and the tile reverts to plain ground). If it doesn't know of any food, it explores instead.
- Terrain is either plain ground or `FOOD` (the only edible terrain); both are traversable. Eaten food does not respawn.

## Build & Run

```bash
# run from IDE or via Gradle
./gradlew run

# build fat jar
./gradlew shadowJar

# run jar
java -jar build/libs/critter-plains-1.0.0-all.jar
```
