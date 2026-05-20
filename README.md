# critter-plains

A creature simulation.

## Structure

- **critters** — simulation logic (world, creatures, map, game loop)
- **visualization** — rendering via [OPENRNDR](https://openrndr.org/)
- **src** — entry point (`Main.kt`)

## Build & Run

```bash
# run from IDE or via Gradle
./gradlew run

# build fat jar
./gradlew shadowJar

# run jar
java -jar build/libs/critter-plains-1.0.0-all.jar
```
