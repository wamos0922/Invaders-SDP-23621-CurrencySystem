# Tempest Queen, Level 20 boss (GOG-75)

The Act 2 boss. Her art (GOG-79) comes from the "Space Invader GoG - Act 2
Jupiter" design system; her numbers come from the Game Design Draft.

## Fight

1. **Warning:** after the level's 6 s start countdown, "WARNING: BOSS
   INCOMING" blinks in red for 2 seconds. She is hidden and can't be hit.
   Her clock starts only when the countdown ends (`Boss.startFight()`, called
   once by `GameScreen`), so nothing runs out during it.
2. **Shield closed (4 s):** the Gas Shield outline is drawn around her.
   Player bullets just disappear (in phase 2 each one calls a mob). She shoots one bullet straight down every
   second.
3. **Shield open (2 s):** the outline disappears. She rests (no shots) and
   every hit takes 1 HP (300 points).
4. Repeat from 2.

**Phase 2** starts at half health (7 of 15 HP): she flashes red/cream for 1
second, then stays cream. The shield stays open for 1.5 s instead of 2 and
she shoots twice as often (every 0.5 s) while it is closed.

## Phase 2 mobs

In phase 2 (after the hit that starts it) every player bullet that reaches
her calls one random Act 2 mob: Polar, Jelly, Beetle, Storm King or Cloud
Ray. That includes bullets blocked by the closed shield, so shooting her
while the shield is closed costs a mob. Bullets that hit a mob first don't
call one.

- Mobs hover in rows of 6 below her and move with her (kept on screen).
- They shoot all the time, shield open or closed, each on its own random
  1.5-2.5 s timer, with their own ability (freeze bullet, cone shot...).
  She keeps her own shots too.
- They stay until killed and stack with every hit; there is no limit. A
  killed mob frees its spot for the next one.
- Killing a mob gives the same points, coin and item chances as a normal
  enemy. A Beetle mob's shield takes the first hit.
- When she is destroyed, every mob dies with her (no points for those).

She moves left and right at the top of the screen (2 px per frame) and
bounces off the edges. Destroying her gives 5000 extra points. Pausing keeps
all her timers still.

All numbers are constants at the top of `entity/TempestQueen.java`.

## Code

- `entity/TempestQueen.java`: the boss, implements `Boss`.
- `entity/Boss.java`: four default methods, `getWarningText()`,
  `startFight()`, `resumeAfterPause(long)` and `getMinions()`. Other bosses don't need to
  change.
- `EnemyShipFormation.fireShot(...)`: one enemy's shot with its ability,
  shared by the formation and her mobs.
- `GameScreen`: level 20 spawns her (other boss levels keep the placeholder),
  draws the warning text, pauses any boss through `Boss`, and checks player
  bullets against boss minions before the boss.
- `DrawManager` / `res/graphics`: sprites `TempestQueen1`, `TempestQueen2`
  (24 x 16) and `GasShield` (28 x 20).

## Testing

Compile `src` and `tests/TempestQueenTest.java` into an isolated build
directory, then run `TempestQueenTest` with that directory and `res` on the
classpath, from a folder that is not your real profile folder.
For manual testing use `-Dgame.startLevel=20`.
