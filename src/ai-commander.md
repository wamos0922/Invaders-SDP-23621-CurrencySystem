# AI Commander, Level 40 boss (GOG-77)

The final boss (Act 4). Sprite from the "Space Invader GoG - Act 4 Deep
Space" design system; attack designs from the project handoff
`claude/level40-boss-attacks-handoff.md` (mockup canvas `Main` / `Phase2`);
HP from the Game Design Draft (30 hits).

## Fight

1. **Warning:** after the level's 6 s start countdown, "WARNING: BOSS
   INCOMING" shows for 2 seconds. She is hidden and can't be hit. Her clock
   starts only when the countdown ends (`Boss.startFight()`).
2. **Phase 1 (30 to 16 HP):** she drifts left and right at the top. Every
   cycle she fires the **Ultimate Weapon** (laser), then rests 1 s. She has
   no normal bullets.
3. **Phase 2 (15 HP and below):** she flashes violet/white for 1 s, then
   stays white (`space-starlight`). The **Claw Slam** comes right away (it
   cancels a laser in progress), then every 30 s. Between slams: laser, then
   **invisible** for 2 s, rest 0.6 s, laser again.

Each hit: 300 points. Destroying her: +5000.

## Ultimate Weapon (laser), 3.4 s cycle

| Time | What happens |
| --- | --- |
| 0.00 | She locks the player's x and glides (6 px/frame) until her jaw is over it. The lock does not follow the player after this. |
| 0.00 to 2.10 s | Charging: WARNING banner blinks, orb pulses at the jaw, dashed aim line flickers down to the ground. Harmless. |
| 2.10 to 2.20 s | Beam grows from the jaw down to the ground. |
| 2.20 to 3.00 s | Firing (flickers 0.8 to 1). Impact burst on the ground. |
| 3.00 to 3.40 s | Fade, pause. Harmless. |

- Damage: a 24 px column on the locked x, from 2.10 s to 3.00 s. At most
  one hit per laser. Move out of the column during the charge.
- Colour: `space-galaxy` #b98aff for the beam, orb, aim line and banner
  (the handoff's default red is not in the Act 4 palette).
- The ground line is 6 px above the bottom of the canvas (just under the
  player's row), so it lines up whatever the window border is.

## Claw Slam (phase 2), 4.0 s

Two segmented arms swing in from the top corners (pivots at the corners,
-100 to 0 degrees, ease-in), the spiked blades meet across the bottom of the
screen at 1.6 s with a small overshoot and bounce, hold, then swing back out.

- **Can't be dodged:** at 1.6 s the player loses 1 life, or only the shield
  (then the bubble pops and "SHIELD BROKEN" floats up in `space-ion`).
  No hit while the ship is already exploding/respawning.
- Impact effects: white full-screen flash (not shaken), screen shake
  (handoff table, 0.4 s), glow and two rings where the blades meet.
- She holds still during the slam.

## Invisible (phase 2)

After each phase 2 laser she disappears for 2 s. She doesn't shoot (nothing
gives away where she is), but player bullets still hit her: each hit shows
her for 0.12 s.

## Code

- `entity/AICommander.java`: the boss (timeline, hits, all drawing with
  Java2D shapes, no new image files).
- `entity/Boss.java`: new default hooks, so other bosses need no change:
  `takePlayerHits(ship)`, `onPlayerHitBlocked()`, `getShakeOffset()`,
  `drawOverlay(drawManager)`.
- `screen/GameScreen.java`: level 40 spawns her; `manageBossAttackHits()`
  applies laser/claw hits (shield first); boss shake is added to the screen
  shake; `drawOverlay` is drawn after the shake is removed.
- `engine/DrawManager.java`: `Commander1`/`Commander2` sprites (end of the
  enum and map, bits at the end of `res/graphics`), `getWorldGraphics()`,
  `getPixelFont(size, bold)`.
- Freeze item: while enemies are frozen her clock stops too, so no attack
  runs out or fires unseen.

## Test

`tests/AICommanderTest.java` (67 checks): sprites, intro, laser lock / glide
/ hit window / one hit per laser / clamp at the edge, phase 2 claw at once
and every 30 s, unavoidable claw hit, shake, invisible (no shots, still
hittable, flash), pause, drawing (banner, beam, claws, flash), kill bonus,
level 40 spawn and a claw costing a life through `GameScreen`.
