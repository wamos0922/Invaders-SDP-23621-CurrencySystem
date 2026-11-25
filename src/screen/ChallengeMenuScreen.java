package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.List;

import engine.Cooldown;
import engine.Core;
import engine.DailyChallenge;

/**
 * Challenges menu (GOG-102): pick the daily challenge (the weekly one comes
 * later, GOG-44). Shows today's stages and the time until they change.
 *
 * Returns {@link #PLAY_DAILY} to start the daily run, or 1 for the title.
 */
public class ChallengeMenuScreen extends Screen {

	/** Code that opens this screen from the title menu. */
	public static final int CODE = 12;
	/** Return code: start the daily challenge. */
	public static final int PLAY_DAILY = 13;

	/** Milliseconds between two menu moves while a key is held. */
	private static final int MOVE_DELAY = 200;
	/** Grey for entries that are not available yet. */
	private static final Color DISABLED = new Color(110, 110, 110);
	/** Grey for secondary text. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** Menu entries. */
	private enum Option {
		/** Play today's daily challenge. */
		DAILY("Daily challenge", true),
		/** Weekly challenge, not built yet (GOG-44). */
		WEEKLY("Weekly challenge - soon", false),
		/** Back to the title screen. */
		BACK("Back", true);

		/** Text shown in the menu. */
		private final String label;
		/** False if it cannot be picked yet. */
		private final boolean enabled;

		Option(final String label, final boolean enabled) {
			this.label = label;
			this.enabled = enabled;
		}
	}

	/** Entry under the cursor. */
	private Option selected;
	/** Limits how fast the cursor moves while a key is held. */
	private final Cooldown moveCooldown;
	/** Today's stages, shown under the menu. */
	private final List<DailyChallenge.Stage> stages;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 */
	public ChallengeMenuScreen(final int width, final int height,
			final int fps) {
		super(width, height, fps);
		this.returnCode = 1;
		this.selected = Option.DAILY;
		this.moveCooldown = Core.getCooldown(MOVE_DELAY);
		this.moveCooldown.reset();
		this.stages = DailyChallenge.stagesForToday();
	}

	/**
	 * Starts the action.
	 *
	 * @return {@link #PLAY_DAILY}, or 1 to go back to the title screen.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Moves the cursor and handles Space and Escape. */
	protected final void update() {
		super.update();
		draw();
		if (!this.inputDelay.checkFinished())
			return;

		if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
			this.returnCode = 1;
			this.isRunning = false;
			return;
		}
		if (this.moveCooldown.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {
				move(-1);
				this.moveCooldown.reset();
			} else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {
				move(1);
				this.moveCooldown.reset();
			}
		}
		if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
			if (this.selected == Option.DAILY) {
				this.returnCode = PLAY_DAILY;
				this.isRunning = false;
			} else if (this.selected == Option.BACK) {
				this.returnCode = 1;
				this.isRunning = false;
			}
		}
	}

	/**
	 * Moves the cursor, wrapping around at the ends.
	 *
	 * @param direction
	 *            -1 for up, 1 for down.
	 */
	private void move(final int direction) {
		Option[] options = Option.values();
		this.selected = options[Math.floorMod(
				this.selected.ordinal() + direction, options.length)];
	}

	/** Draws the menu and today's challenge. */
	private void draw() {
		this.drawManager.initDrawing(this);
		this.drawManager.drawScreenTitle(this, "Challenges");

		int y = this.height / 4;
		for (Option option : Option.values()) {
			boolean isSelected = option == this.selected;
			Color color = !option.enabled ? DISABLED
					: isSelected ? Color.GREEN : Color.WHITE;
			String text = isSelected ? "> " + option.label + " <"
					: option.label;
			this.drawManager.drawCenteredRegularString(this, text, y, color);
			y += 30;
		}

		y += 30;
		this.drawManager.drawCenteredRegularString(this, "Today's daily",
				y, Color.GREEN);
		for (int i = 0; i < this.stages.size(); i++) {
			y += 22;
			this.drawManager.drawCenteredRegularString(this, (i + 1) + ". "
					+ this.stages.get(i).describe(), y, Color.WHITE);
		}
		y += 32;
		this.drawManager.drawCenteredRegularString(this, "New daily in "
				+ DailyChallenge.formatCountdown(DailyChallenge
						.timeUntilReset(LocalDateTime.now())), y, MUTED);

		this.drawManager.drawKeyHints(this,
				"w/s move, space select, esc back");
		this.drawManager.completeDrawing(this);
	}
}
