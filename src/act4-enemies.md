# Act 4 enemies (GOG-86)

Story levels 31-39 (Act 4, Deep Space) use the Deep Space enemies. Sprites
and colours come from the "Space Invader GoG - Act 4 Deep Space" design
system.

| Enemy | Colour | Points | Ability |
| --- | --- | --- | --- |
| Comet | `#5ee6ef` | 30 | Kamikaze |
| Mite | `#7f74ff` | 30 | Boom |
| Stinger | `#b98aff` | 30 | Vulnerable bullet |
| Star Eater | `#f7f2ff` | 20 | None |
| Nebula Wraith | `#ff8fe6` | 10 | None |

## Rows per level

Row counts follow the Game Design Draft (2, 2, 3, 3, 4, 4, 4, 5, 5), with 7
enemies per row. Same pattern as Act 2: levels 31-33 bring in one special
type each, then the types mix and special rows grow until level 39 is all
special. Stinger stays at the back, Comet at the front, Mite in the middle.
Rows are listed top (back) to bottom (front).

Mites are never next to each other, so one boom can't chain: a Mite row
alternates Mite and Star Eater (`EnemyType.MITE_ROW_FILLER`), shifted by one
in each row like a checkerboard. Mites only meet diagonally, and the blast
doesn't reach diagonals.

| Level | Rows |
| --- | --- |
| 31 | Star Eater, **Comet** |
| 32 | Star Eater, **Mite** |
| 33 | **Stinger**, Star Eater, Nebula Wraith |
| 34 | **Stinger**, Nebula Wraith, **Comet** |
| 35 | Star Eater, **Mite**, Nebula Wraith, **Comet** |
| 36 | **Stinger**, Star Eater, **Mite**, **Comet** |
| 37 | **Stinger**, **Mite**, **Mite**, **Comet** |
| 38 | **Stinger**, Star Eater, **Mite**, **Mite**, **Comet** |
| 39 | **Stinger**, **Mite**, **Mite**, **Comet**, **Comet** |

The table is `ACT4_ROWS` in `engine/StoryMode.java`.

## Abilities

- **Kamikaze (Comet):** a normal enemy until the player shoots it. That
  first hit doesn't kill it (no points): it leaves the formation, locks onto
  where the player's ship is and flies there in a straight line at 4 px per
  frame. When it gets there it explodes. The player loses a life if the
  ship's centre is still within 40 px of that spot (`COMET_BLAST_RADIUS`,
  about 2 ship widths of blast), so move away from where you were. A shield
  item blocks it. A second hit while it dives kills it: normal points, no
  explosion. Several hit Comets can dive at once. A Mite blast kills a
  Comet outright (only a player bullet makes it dive).
- **Boom (Mite):** when a Mite dies it sprays 3 shrapnel bullets downward
  (straight, a bit left, a bit right) and every enemy directly left, right,
  above or below it (`MITE_BLAST_RADIUS`, 45 px, not diagonals) takes 1 hit.
  Enemies killed that way give normal points and drops. Since Mites are
  never next to each other, a boom never sets off another Mite.
- **Vulnerable bullet (Stinger):** violet bullet. A hit takes no life, but
  the player is left with only 1 life for 5 s (`VULNERABLE_MILLIS`), then
  the lives come back. "VULNERABLE" and the seconds left show under the
  HUD. Any real hit during those 5 s is a game over. A second Stinger hit
  during them does nothing. A life picked up meanwhile is kept on top.
  Pausing doesn't use up the 5 s, and winning the level gives the lives
  back. It isn't damage, so the shield item doesn't block it. The player
  can shoot Stinger bullets down (both bullets disappear).

Only the lowest living enemy in each column shoots (unchanged rule).

## Code

- `entity/EnemyType`: the 5 types and their numbers.
- `EnemyShipFormation`: Comet dives (`startDive`, `updateDives`,
  `takeDetonations`), Mite checkerboard and deaths (`takeMiteBooms`),
  Stinger shot in `fireShot`.
- `GameScreen`: first hit on a Comet starts its dive, Comet blast check,
  Mite shrapnel, the 1-life period, shooting Stinger bullets down.
- `Bullet`: `setVulnerable` / `isVulnerable`.
- `DrawManager` / `res/graphics`: 10 sprites (2 frames each).

## Testing

Compile `src` and `tests/Act4EnemyTest.java` into an isolated build
directory, then run `Act4EnemyTest` with that directory and `res` on the
classpath, from a folder that is not your real profile folder.
For manual testing use `-Dgame.startLevel=31` (up to 39).
