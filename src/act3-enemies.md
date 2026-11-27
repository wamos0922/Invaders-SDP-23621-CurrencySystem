# Act 3 enemies (GOG-28, GOG-85)

Story levels 21-29 (Act 3, Saturn) use the Saturn enemies. Sprites and
colours come from the "Space Invader GoG - Act 3 Saturn" design system
(GOG-28); abilities are GOG-85.

| Row | Enemy | Colour | Points | Ability |
| --- | --- | --- | --- | --- |
| 1 | Ring Commander | `#ece6d2` | 50 | None |
| 2 | Moonlet Pod | `#9a7564` | 40 | None |
| 3 | Halo Lancer | `#e8c867` | 30 | Sniper |
| 4 | Orbit Fighter | `#d0913f` | 20 | Drop |
| 5 | Hex Sentinel | `#6fb3ae` | 10 | Reflecting shield |

## Rows per level

Like Act 1: a level with N rows uses the first N enemies of the table, from
the top. Row counts follow the Game Design Draft (2, 2, 3, 3, 4, 4, 4, 5, 5),
6 enemies per row, no two-hit enemies. So the abilities arrive with their
rows: Halo Lancer from level 23, Orbit Fighter from 25, Hex Sentinel in 28
and 29. Level 30 is the boss level.

The table is `ACT3_ORDER` / `ACT3_ROWS` in `engine/StoryMode.java`.

## Abilities

- **Sniper (Halo Lancer):** its shot is fast (8 px per frame, twice a normal
  enemy bullet) and aimed straight at where the player is, with no warning.
  Like every enemy, it only shoots while it is the lowest living enemy in
  its column.
- **Drop (Orbit Fighter):** the first player bullet that hits it doesn't
  kill it (no points): it leaves the formation and falls straight down at
  3 px per frame. If it touches the player's ship, the player loses a life
  (a shield item blocks it) and the fighter is gone. If it misses, it falls
  off the bottom of the screen and is gone (no points). A second hit while
  it falls kills it: normal points. Unlike the Act 4 Comet, it isn't aimed
  and doesn't explode; the player just steps aside.
- **Reflecting shield (Hex Sentinel):** it starts with a one-hit shield,
  drawn in the shield colour like the Act 2 Beetle. The first player bullet
  breaks the shield and comes back down as an enemy bullet aimed at the
  player (normal speed, 4). Then the Hex is back to its own colour and the
  next hit kills it.

## Code

- `entity/EnemyType`: the 5 types, `FIGHTER_DROP_SPEED`,
  `SNIPER_BULLET_SPEED`, `REFLECT_BULLET_SPEED`; Hex Sentinel has 1 shield
  hit.
- `EnemyShipFormation`: Orbit Fighter drop reuses the Comet dive (`startDive`
  starts it, `updateDives` moves it, `takeDropHits` checks the player,
  `isDropping`); `setTarget` and the Halo Lancer shot in `fireShot`;
  `reflectShot` for the Hex.
- `GameScreen`: gives the formation the player's position, reflects the
  bullet that breaks a Hex shield, and takes a life for a falling fighter
  (`handleFighterDrops`).
- `DrawManager` / `res/graphics`: 10 sprites (2 frames each), from GOG-28.

## Testing

Compile `src` and `tests/Act3EnemyTest.java` into an isolated build
directory, then run `Act3EnemyTest` with that directory and `res` on the
classpath, from a folder that is not your real profile folder. For manual
testing use `-Dgame.startLevel=28` (all five rows) or 23 / 25 for one
ability at a time.
