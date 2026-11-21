# Stamina system

This document describes the player-facing stamina rules implemented for GOG-11.

## Shared stamina rules

- Maximum stamina: 5.
- Regeneration: 1 stamina every 10 minutes of real time, including while the game is closed.
- Diamond refill: spend 1 in-game diamond to refill stamina to 5; a full bar cannot be refilled.
- Tester mode may bypass stamina restrictions for development and testing.

## Story Mode

- Story Mode consumes 1 stamina for **each level attempt**.
- The first level costs 1 stamina when the player starts a new story or loads a save.
- Clearing a level does not charge again immediately; the next stamina is consumed immediately before the next level begins.
- If the player runs out of lives and chooses Play Again, the restarted attempt costs another 1 stamina, whether continuing the same saved level or choosing another save slot.
- If no stamina remains before a level starts, that level is not launched. The player is returned to the title screen and shown the Energy Status popup. Progress from the previously cleared level remains checkpointed.

## Endless Mode

- Endless Mode consumes 1 stamina for the entire run.
- Reaching additional waves/levels during that run does not consume more stamina.
- Dying or quitting ends the run. Choosing Play Again starts a new run and consumes another 1 stamina.

## Title menu behaviour

- When stamina is above zero, Story and Endless are displayed normally: white when unselected and green when selected.
- At zero stamina, both entries are disabled visually in the same grey as the `BY GOG` credit, whether selected or unselected.
- Selecting either unavailable mode still opens the Energy Status popup so the player can inspect the regeneration countdown.
- Other title-menu entries remain available at zero stamina.
