# Act 2 enemies (GOG-84)

Story levels 11-19 (Act 2, Jupiter) use the Jupiter enemies instead of the
original A/B/C ships. Sprites and colours come from the
"Space Invader GoG - Act 2 Jupiter" design system.

| Enemy | Colour | Points | Ability |
| --- | --- | --- | --- |
| Polar | `#9fb8d6` | 30 | Freeze bullet |
| Jelly | `#f3e4c4` | 30 | Double shot |
| Beetle | `#ad6a40` | 30 | Shield |
| Storm King | `#ec6a45` | 20 | None |
| Cloud Ray | `#d9a24f` | 10 | None |

## Rows per level

Row counts follow the Game Design Draft (2, 2, 3, 3, 4, 4, 4, 5, 5), with 5
enemies per row. Levels 11-13 bring in one special type each; from level 14
the types mix and the number of special rows grows. Polar stays at the back
and Beetle at the front. Rows are listed top (back) to bottom (front).

| Level | Rows |
| --- | --- |
| 11 | Storm King, **Beetle** |
| 12 | Storm King, **Jelly** |
| 13 | **Polar**, Storm King, Cloud Ray |
| 14 | **Polar**, Cloud Ray, **Beetle** |
| 15 | Storm King, **Jelly**, Cloud Ray, **Beetle** |
| 16 | **Polar**, Storm King, **Jelly**, **Beetle** |
| 17 | **Polar**, **Jelly**, **Jelly**, **Beetle** |
| 18 | **Polar**, Storm King, **Jelly**, **Jelly**, **Beetle** |
| 19 | **Polar**, **Jelly**, **Jelly**, **Beetle**, **Beetle** |

The table is `ACT2_ROWS` in `engine/StoryMode.java`. Level 15 is a normal
level (no mid boss), as the draft says. Speed, fire rate and diagonal levels
(13, 16, 19) are unchanged; there are no two-hit enemies in Act 2 yet.

## Abilities

- **Freeze bullet (Polar):** drawn in light blue `#4AB9E8`. A hit takes no
  life: the ship turns the same blue and can't move or shoot for 3 seconds
  (`EnemyType.FREEZE_MILLIS`). Items can still be used. A freeze that lands
  while the ship is already frozen is ignored, and pausing doesn't use up the
  freeze. The shield item does not block it, since it isn't damage. A normal
  bullet still takes a life while the ship is frozen.
- **Double shot (Jelly):** fires two bullets from the same point, one drifting
  left and one right, so they spread in a cone (`EnemyType.DOUBLE_SHOT_SPREAD`,
  1 px sideways per frame, about 14 degrees each side).
- **Shield (Beetle):** absorbs 1 hit (`EnemyType.BEETLE_SHIELD_HITS`). A
  shielded Beetle is drawn cyan (`EnemyType.SHIELD_COLOR`); when the shield
  breaks it turns brown. Breaking the shield gives no points or drops.

Only the lowest living enemy in each column shoots (unchanged rule), so the
front row fires first and Polars at the back fire once the enemies below them
are gone.

## Adding Act 3 / Act 4 types

Add the type to `entity/EnemyType` (sprites, colour, points, shield hits),
handle its shot in `EnemyShipFormation.shoot`, and give the levels a row table
with `GameSettings.withRowTypes(...)` in `StoryMode`.

## Testing

Compile `src` and `tests/Act2EnemyTest.java` into an isolated build
directory, then run `Act2EnemyTest` with that directory and `res` on the
classpath, from a folder that is not your real profile folder.
For manual testing use `-Dgame.startLevel=11` (up to 19).
