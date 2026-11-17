
package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import engine.Cooldown;
import engine.Core;
import engine.GameState;
import engine.Score;

/**
 * Displays game-over results.
 *
 * Endless Mode: high-score entry and saving enabled.
 * Story Mode: results only, no leaderboard recording.
 */
public class ScoreScreen extends Screen {

	private static final int SELECTION_TIME = 200;
	private static final int MAX_HIGH_SCORE_NUM = 7;
	private static final int FIRST_CHAR = 65;
	private static final int LAST_CHAR = 90;

	private int score;
	private int livesRemaining;
	private int bulletsShot;
	private int shipsDestroyed;

	private List<Score> highScores;
	private boolean isNewRecord;

	private final boolean highScoresEnabled;

	private char[] name;
	private int nameCharSelected;
	private Cooldown selectionCooldown;

	/**
	 * Original constructor.
	 * Preserves high-score functionality by default.
	 */
	public ScoreScreen(
			final int width,
			final int height,
			final int fps,
			final GameState gameState) {

		this(width, height, fps, gameState, true);
	}

	/**
	 * Constructor with high-score control.
	 *
	 * @param highScoresEnabled true for Endless Mode,
	 *                          false for Story Mode.
	 */
	public ScoreScreen(
			final int width,
			final int height,
			final int fps,
			final GameState gameState,
			final boolean highScoresEnabled) {

		super(width, height, fps);

		this.highScoresEnabled = highScoresEnabled;

		this.score = gameState.getScore();
		this.livesRemaining = gameState.getLivesRemaining();
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();

		Core.getAchievementManager().recordAccuracy(
				this.bulletsShot,
				this.shipsDestroyed,
				gameState.getLevelsCompletedRun()
		);

		this.isNewRecord = false;
		this.name = "AAA".toCharArray();
		this.nameCharSelected = 0;

		this.selectionCooldown =
				Core.getCooldown(SELECTION_TIME);

		this.selectionCooldown.reset();

		// Only Endless Mode loads and checks high scores.
		if (this.highScoresEnabled) {
			try {
				this.highScores =
						Core.getFileManager().loadHighScores();

				if (highScores.size() < MAX_HIGH_SCORE_NUM
						|| highScores.get(
						highScores.size() - 1
				).getScore() < this.score) {

					this.isNewRecord = true;
				}

			} catch (IOException e) {
				logger.warning(
						"Couldn't load high scores!"
				);
			}
		}
	}

	@Override
	public final int run() {
		super.run();
		return this.returnCode;
	}

	@Override
	protected final void update() {
		super.update();

		draw();

		if (this.inputDelay.checkFinished()) {

			if (inputManager.isKeyDown(
					KeyEvent.VK_ESCAPE)) {

				// Return to main menu.
				this.returnCode = 1;
				this.isRunning = false;

				if (this.highScoresEnabled
						&& this.isNewRecord) {
					saveScore();
				}

			} else if (inputManager.isKeyDown(
					KeyEvent.VK_SPACE)) {

				// Play again.
				this.returnCode = 2;
				this.isRunning = false;

				if (this.highScoresEnabled
						&& this.isNewRecord) {
					saveScore();
				}
			}

			// High-score name editing:
			// Endless Mode only.
			if (this.highScoresEnabled
					&& this.isNewRecord
					&& this.selectionCooldown.checkFinished()) {

				if (inputManager.isKeyDown(
						KeyEvent.VK_RIGHT)) {

					this.nameCharSelected =
							this.nameCharSelected == 2
									? 0
									: this.nameCharSelected + 1;

					this.selectionCooldown.reset();
				}

				if (inputManager.isKeyDown(
						KeyEvent.VK_LEFT)) {

					this.nameCharSelected =
							this.nameCharSelected == 0
									? 2
									: this.nameCharSelected - 1;

					this.selectionCooldown.reset();
				}

				if (inputManager.isKeyDown(
						KeyEvent.VK_UP)) {

					this.name[this.nameCharSelected] =
							(char) (
									this.name[this.nameCharSelected]
											== LAST_CHAR
											? FIRST_CHAR
											: this.name[this.nameCharSelected] + 1
							);

					this.selectionCooldown.reset();
				}

				if (inputManager.isKeyDown(
						KeyEvent.VK_DOWN)) {

					this.name[this.nameCharSelected] =
							(char) (
									this.name[this.nameCharSelected]
											== FIRST_CHAR
											? LAST_CHAR
											: this.name[this.nameCharSelected] - 1
							);

					this.selectionCooldown.reset();
				}
			}
		}
	}

	/**
	 * Saves leaderboard scores for Endless Mode only.
	 */
	private void saveScore() {

		if (!this.highScoresEnabled
				|| this.highScores == null) {
			return;
		}

		highScores.add(
				new Score(new String(this.name), score)
		);

		Collections.sort(highScores);

		if (highScores.size() > MAX_HIGH_SCORE_NUM) {
			highScores.remove(highScores.size() - 1);
		}

		try {
			Core.getFileManager()
					.saveHighScores(highScores);

		} catch (IOException e) {
			logger.warning(
					"Couldn't save high scores!"
			);
		}
	}

	/**
	 * Draws the game-over results.
	 */
	private void draw() {

		drawManager.initDrawing(this);

		boolean showRecord =
				this.highScoresEnabled && this.isNewRecord;

		drawManager.drawGameOver(
				this,
				this.inputDelay.checkFinished(),
				showRecord
		);

		// Prevent division by zero.
		float accuracy =
				this.bulletsShot > 0
						? (float) this.shipsDestroyed
						  / this.bulletsShot
						: 0f;

		drawManager.drawResults(
				this,
				this.score,
				this.livesRemaining,
				this.shipsDestroyed,
				accuracy,
				showRecord
		);

		if (showRecord) {
			drawManager.drawNameInput(
					this,
					this.name,
					this.nameCharSelected
			);
		}

		drawManager.completeDrawing(this);
	}
}
