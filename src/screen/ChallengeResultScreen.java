package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

import engine.Core;
import engine.DailyChallenge;
import engine.WeeklyChallenge;

/**
 * Results of a daily or weekly challenge run (GOG-102, GOG-44): cleared or
 * failed, stages cleared, score and reward. Space or Enter goes back to the
 * challenges menu.
 */
public class ChallengeResultScreen extends Screen {

	/** Milliseconds before the screen can be closed. */
	private static final int CLOSE_DELAY = 800;
	/** Grey for secondary text. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** True for the weekly challenge, false for the daily one. */
	private final boolean weekly;
	/** True if every stage was cleared. */
	private final boolean cleared;
	/** Stages cleared. */
	private final int stagesCleared;
	/** Stages in the run. */
	private final int stageCount;
	/** Final score. */
	private final int score;
	/** Reward text, or null if the run failed. */
	private final String reward;
	/** True once Space and Enter are released since the screen opened. */
	private boolean keysReleased;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 * @param weekly
	 *            True for the weekly challenge, false for the daily one.
	 * @param cleared
	 *            True if every stage was cleared.
	 * @param stagesCleared
	 *            Stages cleared.
	 * @param stageCount
	 *            Stages in the run.
	 * @param score
	 *            Final score.
	 * @param reward
	 *            Reward text, or null if the run failed.
	 */
	public ChallengeResultScreen(final int width, final int height,
			final int fps, final boolean weekly, final boolean cleared,
			final int stagesCleared,
			final int stageCount, final int score, final String reward) {
		super(width, height, fps);
		this.weekly = weekly;
		this.cleared = cleared;
		this.stagesCleared = stagesCleared;
		this.stageCount = stageCount;
		this.score = score;
		this.reward = reward;
		this.keysReleased = false;
		this.returnCode = ChallengeMenuScreen.CODE;
		this.inputDelay = Core.getCooldown(CLOSE_DELAY);
		this.inputDelay.reset();
	}

	/**
	 * Starts the action.
	 *
	 * @return Code of the challenges menu.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Closes on Space or Enter. */
	protected final void update() {
		super.update();
		draw();

		boolean confirm = inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| inputManager.isKeyDown(KeyEvent.VK_ENTER);
		if (!this.keysReleased) {
			this.keysReleased = !confirm;
			return;
		}
		if (this.inputDelay.checkFinished() && confirm)
			this.isRunning = false;
	}

	/** Draws the results. */
	private void draw() {
		this.drawManager.initDrawing(this);
		String mode = this.weekly ? "Weekly" : "Daily";
		this.drawManager.drawScreenTitle(this,
				mode + (this.cleared ? " cleared!" : " failed"));

		int y = this.height / 3;
		this.drawManager.drawCenteredRegularString(this, "Stages cleared: "
				+ this.stagesCleared + " / " + this.stageCount, y,
				Color.WHITE);
		y += 28;
		this.drawManager.drawCenteredRegularString(this, "Score: "
				+ this.score, y, Color.WHITE);
		y += 40;
		if (this.reward != null)
			this.drawManager.drawCenteredRegularString(this, this.reward, y,
					Color.GREEN);
		else
			this.drawManager.drawCenteredRegularString(this,
					"Clear every stage to earn the reward", y, MUTED);
		y += 40;
		LocalDateTime now = LocalDateTime.now();
		this.drawManager.drawCenteredRegularString(this, "New "
				+ mode.toLowerCase() + " in "
				+ DailyChallenge.formatCountdown(this.weekly
						? WeeklyChallenge.timeUntilReset(now)
						: DailyChallenge.timeUntilReset(now)), y, MUTED);

		this.drawManager.drawKeyHints(this, "space continue");
		this.drawManager.completeDrawing(this);
	}
}
