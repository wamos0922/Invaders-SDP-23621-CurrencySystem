# AI Warship, Level 10 boss (GOG-74)

The Act 1 (Earth) boss. Its art (GOG-78) comes from the "Space Invader GoG"
design system; its numbers come from the Game Design Draft.

## Fight

1. **Fly-in:** after the level's 6 s start countdown, it flies down from
   above the screen for 2 s while "WARNING: BOSS INCOMING" blinks. It can't
   be hit and doesn't shoot. Its clock starts only when the countdown ends
   (`Boss.startFight()`).
2. **Phase 1 (15 to 8 HP):** it moves left and right at the top, at 1.6 px
   per frame: twice the fastest normal formation (8 px every 10 frames). It
   bounces off the edges and never moves down. Every 1.2 s it fires two
   bullets, one from each cannon, aimed at the player. Colour `earth-ocean`.
3. **Phase 2 (7 HP and below):** it flashes for 1 s, then stays
   `earth-magma`. Four Repair Drones fly down from the top into a row under
   it and "DESTROY THE REPAIR DRONES" blinks for 2.5 s. It keeps moving and
   shooting.
4. **Drones:** 2 hits each, `earth-land`, they don't shoot. They follow
   their spot under the boss slowly (1 px per frame) and sway side to side.
   Each living drone heals the boss 1 HP every 3 s, **up to full health**
   (it glows green on a heal). While any drone is alive the boss is
   **invincible**: bullets that reach it just disappear. Drones come only
   once. A drone kill gives 100 points plus the normal coin and item
   chances.
5. **Finish:** with every drone dead, hits land again until it explodes.

Each hit that lands gives 300 points; destroying it gives 5000 more.
Pausing keeps every timer still.

## Black hole (win)

When it is destroyed the level counts as cleared, and `BlackHoleScene`
plays before the level ends:

1. 1 s: the boss explodes (leftover bullets are cleared).
2. 0.8 s: a black hole opens where it was.
3. 1.8 s: the player's ship is pulled in, shrinking to nothing.
4. 0.7 s: the screen fades to black.

Then the level ends as usual and the Act 1 story panels after Level 10
(black hole, new planet) follow.

All numbers are constants at the top of `entity/AIWarship.java` and
`screen/BlackHoleScene.java`.

## Code

- `entity/AIWarship.java`: the boss, implements `Boss`.
- `entity/EnemyType.java`: `REPAIR_DRONE`, which keeps its design colour at
  2 HP instead of turning yellow (`showsExtraHealth()`).
- `screen/BlackHoleScene.java`: the black hole timeline and the pull.
- `GameScreen`: level 10 spawns it; starts, pauses, draws and waits for the
  black hole scene.
- `DrawManager.drawBlackHole(...)`: draws the hole.
- `DrawManager` / `res/graphics`: sprites `AIWarship1`, `AIWarship2`
  (24 x 16) and `RepairDrone1`, `RepairDrone2` (12 x 8), from GOG-78.

## Testing

Compile `src` and `tests/AIWarshipTest.java` into an isolated build
directory, then run `AIWarshipTest` with that directory and `res` on the
classpath, from a folder that is not your real profile folder. The last
part (the black hole inside `GameScreen`) opens a game window, so it is
skipped on a machine without a display. For manual testing use
`-Dgame.startLevel=10`.
