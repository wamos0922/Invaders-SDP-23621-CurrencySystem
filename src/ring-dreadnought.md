# Ring Dreadnought, Level 30 boss (GOG-76)

The Act 3 (Saturn) boss. Its art (GOG-81) comes from the "Space Invader GoG
- Act 3 Saturn" design system; its numbers come from the Game Design Draft.

## Fight

1. **Fly-in:** after the level's 6 s start countdown it flies down from
   above the screen for 2 s while "WARNING: BOSS INCOMING" blinks. It can't
   be hit and doesn't shoot.
2. **Movement:** left and right at 1.5 px per frame, bouncing off the edges,
   and a slow bob up and down of 12 px (one bob every 4 s).
3. **Phase 1 (20 to 11 HP), colour `saturn-gold`:**
   - **Wing fans** every 2 s: each wing tip fires 3 bullets (speed 4) that
     spread outwards (the inner one goes straight down).
   - **Spreading burst** every 3 s, between the fans: the centre cannon fires
     4 bullets (speed 3) from one point that open up as they fall.
4. **Phase 2 (10 HP and below):** it flashes for 1 s, then stays
   `saturn-butterscotch`. Fans come every 1.2 s; bursts keep going.
   - **Shotgun swoop** every 4 s: it flashes white for 0.6 s (warning), dives
     in 0.7 s until its lower edge is 150 px above the player, fires 5 close
     pellets (speed 5), stays down for 0.3 s, then flies back up in 0.8 s.
     Pellets vanish after 200 px (`Bullet.setMaxY`), just past the player's
     row. Fans and bursts wait while it swoops and for a moment after.
     It can be hit during the swoop.

**Gaps:** every fan and burst leaves at least 1.5 player ship widths (39 px)
of empty space between bullets at the player's row, wherever the boss is.
That's why it doesn't fire fans or bursts from low down during a swoop.

Each hit gives 300 points; destroying it gives 5000 more. It explodes and
the level ends; the story panels carry on. Pausing keeps every timer still.

All numbers are constants at the top of `entity/RingDreadnought.java`.

## Code

- `entity/RingDreadnought.java`: the boss, implements `Boss`.
- `entity/Bullet.java` / `BulletPool.java`: `setMaxY` / `getMaxY`, a
  short range for a bullet (reset when a bullet is reused).
- `GameScreen`: level 30 spawns it; `cleanBullets` also removes a bullet
  past its range.
- `DrawManager` / `res/graphics`: sprites `RingDreadnought1`,
  `RingDreadnought2` (24 x 16), from GOG-81.

## Testing

Compile `src` and `tests/RingDreadnoughtTest.java` into an isolated build
directory, then run `RingDreadnoughtTest` with that directory and `res` on
the classpath, from a folder that is not your real profile folder. For
manual testing use `-Dgame.startLevel=30`.
