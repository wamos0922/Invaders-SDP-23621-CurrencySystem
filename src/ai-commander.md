# AI Commander, Level 40 boss (GOG-77)

The final boss (Act 4): a rogue AI mothership that wrecks the city. The art
follows the team's reference picture (grey hull lit red, halo, spires,
pointed wings, glowing core); phase 2 is a second form (torn wings, shattered
halo, open eye, open jaw). HP from the Game Design Draft (30 hits). Every
tunable number is a constant at the top of `entity/AICommander.java`.

## Fight

1. **Warning:** after the level's 6 s start countdown, "WARNING: BOSS
   INCOMING" and "DON'T DRAG THIS FIGHT" show together for 3 s. She is
   hidden; bullets fly through. Her clock starts only when the countdown
   ends (`Boss.startFight()`).
2. **Phase 1 (30 to 16 HP):** she drifts left and right. Wing turrets take
   turns firing aimed 3-way spreads (every 1.1 s). Big attacks take turns,
   with a 0.9 s rest between: **laser**, then **halo shockwave**.
3. **Phase 2 (15 HP and below):** she flickers between both forms for 1 s,
   her halo shatters, and the first **escort barrage** comes at once (it
   cancels a laser or shockwave in progress). Then she is shown 1.5 s,
   hidden 3.5 s, over and over (hidden about 70% of the time):
   - shown: the eye fires a stream of 4 aimed bullets;
   - hidden: one attack per hidden stretch, taking turns: **triple laser**,
     then **halo shards**;
   - barrages keep coming on a shrinking timer (below).

Each hit on the weak point: 300 points. Destroying her: +5000. Her escorts
die with her (no points). She breaks up in a chain of explosions.

## Weak point

Only her **glowing core** can be hurt. A bullet hurts her if its centre is
within 20 px of her centre line: the whole glow around the core and the
glowing column below it (the spike in phase 1, the open jaw in phase 2).
What glows can be hit, at any time. Bullets that hit the
rest of the hull only spark. Bullets fly through the gaps in her art (under
the wings, between the spires). The core pulses so it's obvious where to
shoot; while she's hidden, nothing shows (a hit flashes her for 0.12 s).

## Laser

| Time | Phase 1 | Phase 2 (hidden) |
| --- | --- | --- |
| Start | Locks the player's x. She glides (6 px/frame) until her spike is over it, leaving the screen partly if needed: the corners aren't safe. | Same lock. Three beams: the locked one from the jaw, two more 76 px left and right from the wing tips. |
| Charge | 2.1 s: WARNING banner, orb, dashed aim line. Harmless. | 0.6 s: only a faint flicker at each beam's start. No banner, no aim line. |
| Fire | Beam grows to the ground in 0.1 s, hurts for 0.8 s. | Same. |
| Fade | 0.4 s, harmless. | Same. |

24 px wide beams. At most one hit per laser. The gap between phase 2 beams
is 52 px (the ship is 26 px). Turning the phase 2 tell to 300 ms (or 0)
makes it harder.

## Halo shockwave

Her halo glows, then a wave drops from it to the ground, curving out to
cover the whole width, with one gap. Be fully inside the gap when it passes
your row, or lose a life (one hit per wave). The gap opens at least 90 px
from where the player is when it starts, so you have to move. White posts
on the ground mark the gap's edges.

| | Phase 1 (halo) | Phase 2 (halo shards) |
| --- | --- | --- |
| Glow | 0.6 s | 0.3 s |
| Fall | 1.4 s | 1.0 s |
| Gap | 56 px | 48 px |
| Gap posts | from the start | once it falls |

## Escort barrage (phase 2), 4.6 s

| Time | What happens |
| --- | --- |
| 0.0 to 1.0 s | Six escort fighters fly in from both sides and line up just under her, wing tip to wing tip: the wall is exactly her width, no gaps. She holds still (she first glides back on screen if a laser left her at the edge). |
| 1.0 to 2.2 s | Hex shields: the fighters can't be shot down. Their cores charge up. |
| 2.2 s | Shields drop and every fighter fires 2 missiles (12 in all, spread over the whole width). |
| 3.0 s | Missiles hit the ground: **unavoidable**. The player loses 1 life, or only the shield ("SHIELD BROKEN"). Flash and screen shake. |
| 2.2 to 3.8 s | Fighters take 2 hits each. Shoot the middle ones to open a way to her core. |
| 3.8 to 4.6 s | Survivors fly back out. |

The first barrage comes when phase 2 starts. After each one ends, the next
comes 15 s later, then 10 s, then 5 s, then every 3 s. No laser or shockwave
starts if it would still run when the next barrage is due, and none starts
during one.

## Code

- `entity/AICommander.java`: the boss (timeline, hits, escorts, all drawing:
  the art is pixel strings in the file, drawn at 2x like every sprite).
- `entity/Boss.java`: default hooks, so other bosses need no change:
  `takePlayerHits(ship)`, `onPlayerHitBlocked()`, `getShakeOffset()`,
  `drawOverlay(drawManager)`, `drawBody(drawManager)` (a boss with its own
  art draws itself), `isHitBy(bullet)` (bullets fly through gaps).
- `screen/GameScreen.java`: level 40 spawns her centred under the boss bar;
  `manageBossAttackHits()` applies laser, shockwave and barrage hits (Armor
  skill nodes, then the shield item); boss shake is added to the screen
  shake; a boss warning can have several lines (split by `\n`).
- `engine/DrawManager.java`: `getWorldGraphics()`, `getPixelFont(size, bold)`.
- Freeze item: while enemies are frozen her clock stops too, and she
  doesn't shoot.
- Escorts are `EnemyShip`s (Star Eater type, drawn with their own art) given
  through `getMinions()`, so `GameScreen` hits them like the Tempest Queen's
  mobs.
- Testing: `java -Dcommander.phase2=true -Dgame.startLevel=40 -cp "bin;res"
  engine.Core` starts her straight in phase 2.

## Test

`tests/AICommanderTest.java` (139 checks): intro, art, weak point and gaps,
turrets, laser lock / glide / corners / hit window, halo gap (also at the
far screen edge), phase 2 entry,
escort wall (no gaps, shields, 2 hits), unavoidable barrage, the 15 / 10 / 5
/ 3 s schedule over 80 s, hidden share, triple quiet laser, halo shards, eye
stream, dev switch, pause, freeze, drawing every state, kill, and through
`GameScreen`: level 40 spawn, bullets through a gap / on the core / on the
hull, and a barrage costing a life.
