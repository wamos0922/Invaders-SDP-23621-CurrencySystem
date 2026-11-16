package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Endless mode (GOG-7): waves keep coming until the player runs out of
 * lives. Wave N is generated here instead of read from the fixed level
 * list in Core.
 *
 * Other features plug into the end of each wave through
 * {@link LevelClearedListener}. Example, from the card system (GOG-9):
 *
 * <pre>
 * EndlessMode.addLevelClearedListener(new EndlessMode.LevelClearedListener() {
 *     public GameState onLevelCleared(GameState state, int wave) {
 *         // show the pick-1-of-3 screen, apply the card, return the state
 *         return state;
 *     }
 * });
 * </pre>
 *
 * Until the mode menu (GOG-13) exists, start endless mode with
 * {@code -Dgame.mode=endless}.
 */
public final class EndlessMode {

	/** Called by Core after every wave the player clears alive. */
	public interface LevelClearedListener {
		/**
		 * Runs between waves, after the cleared wave's screen has closed
		 * and before the next wave starts, so it may show its own screen.
		 *
		 * @param state
		 *            State going into the next wave.
		 * @param clearedWave
		 *            Number of the wave that was just cleared (1-based).
		 * @return State to start the next wave with; return
		 *         {@code state} unchanged if nothing needs to change.
		 */
		GameState onLevelCleared(GameState state, int clearedWave);
	}

	/** Largest formation, kept to the size of the biggest story level. */
	private static final int MAX_FORMATION_WIDTH = 7;
	/** Largest formation height. */
	private static final int MAX_FORMATION_HEIGHT = 5;
	/** Fastest enemy movement (lower is faster). */
	private static final int MIN_BASE_SPEED = 5;
	/** Most frequent enemy shooting, in milliseconds (lower is more). */
	private static final int MIN_SHOOTING_FRECUENCY = 400;

	/** Whether the current run is an endless run. */
	private static boolean enabled = "endless".equalsIgnoreCase(
			System.getProperty("game.mode", "story"));

	/** Hooks run after each cleared wave. */
	private static final List<LevelClearedListener> LISTENERS =
			new ArrayList<LevelClearedListener>();

	/** Not used. Static class. */
	private EndlessMode() {
	}

	/** @return True if runs are played in endless mode. */
	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * Switches between endless and story runs. The mode menu (GOG-13)
	 * should call this before starting a run.
	 *
	 * @param endless
	 *            True for endless mode, false for story mode.
	 */
	public static void setEnabled(final boolean endless) {
		enabled = endless;
	}

	/**
	 * Builds the settings for one wave. Waves get bigger, faster and
	 * shoot more often, up to fixed caps. This is a first version; the
	 * difficulty curve gets tuned in week 2.
	 *
	 * @param wave
	 *            Wave number, starting at 1.
	 * @return Settings for that wave.
	 */
	public static GameSettings getSettings(final int wave) {
		int w = Math.max(1, wave) - 1;
		int width = Math.min(5 + w / 4, MAX_FORMATION_WIDTH);
		int height = Math.min(3 + w / 3, MAX_FORMATION_HEIGHT);
		int baseSpeed = Math.max(60 - w * 6, MIN_BASE_SPEED);
		int shooting = Math.max(2000 - w * 150, MIN_SHOOTING_FRECUENCY);
		boolean diagonal = wave % 3 == 0;
		return new GameSettings(width, height, baseSpeed, shooting, diagonal);
	}

	/**
	 * Adds a hook that runs after every cleared wave.
	 *
	 * @param listener
	 *            Hook to add.
	 */
	public static void addLevelClearedListener(
			final LevelClearedListener listener) {
		if (listener != null && !LISTENERS.contains(listener))
			LISTENERS.add(listener);
	}

	/**
	 * Removes a hook added with
	 * {@link #addLevelClearedListener(LevelClearedListener)}.
	 *
	 * @param listener
	 *            Hook to remove.
	 */
	public static void removeLevelClearedListener(
			final LevelClearedListener listener) {
		LISTENERS.remove(listener);
	}

	/**
	 * Runs every hook in the order they were added. Each hook gets the
	 * state returned by the previous one. Called by Core.
	 *
	 * @param state
	 *            State going into the next wave.
	 * @param clearedWave
	 *            Wave that was just cleared.
	 * @return State to start the next wave with.
	 */
	public static GameState fireLevelCleared(final GameState state,
			final int clearedWave) {
		GameState result = state;
		for (LevelClearedListener l
				: new ArrayList<LevelClearedListener>(LISTENERS)) {
			GameState next = l.onLevelCleared(result, clearedWave);
			if (next != null)
				result = next;
		}
		return result;
	}
}
