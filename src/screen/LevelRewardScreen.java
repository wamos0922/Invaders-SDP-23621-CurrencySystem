package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;

import engine.Core;
import engine.CurrencyManager;
import engine.DiamondManager;
import engine.LevelRewards;

/**
 * Shown after a story boss level is cleared for the first time (GOG-38),
 * before the story panels and the cash-out screen. It shows the bonus that
 * was just banked and waits for Space or Enter.
 *
 * Opened directly by Core between levels, not from the main menu.
 */
public class LevelRewardScreen extends Screen {

	/** Milliseconds before the screen can be closed. */
	private static final int CLOSE_DELAY = 600;
	/** Colour of the "first clear" line. */
	private static final Color MUTED = new Color(170, 170, 170);

	/** Level that was just cleared. */
	private final int clearedLevel;
	/** The bonus that was paid. */
	private final LevelRewards.Reward reward;
	/** True once Space and Enter are released since the screen opened, so
	 * the Space held for shooting cannot close it by accident. */
	private boolean keysReleased;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 * @param clearedLevel
	 *            Story level that was just cleared.
	 * @param reward
	 *            The bonus that was paid for it.
	 */
	public LevelRewardScreen(final int width, final int height, final int fps,
			final int clearedLevel, final LevelRewards.Reward reward) {
		super(width, height, fps);
		this.clearedLevel = clearedLevel;
		this.reward = reward;
		this.keysReleased = false;
		this.inputDelay = Core.getCooldown(CLOSE_DELAY);
		this.inputDelay.reset();
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code (unused: Core opens this screen directly).
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Updates the screen and closes it on Space or Enter. */
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

	/** Draws the bonus and the new balances. */
	private void draw() {
		this.drawManager.initDrawing(this);

		this.drawManager.drawScreenTitle(this, this.reward.getTitle());
		this.drawManager.drawCenteredRegularString(this, "Level "
				+ this.clearedLevel + " first clear bonus",
				this.height / 4, MUTED);

		int y = this.height / 3 + 10;
		this.drawManager.drawCoinBalance(this, this.reward.getCoins(), y);
		this.drawManager.drawCenteredRegularString(this, "bonus coins",
				y + 25, Color.WHITE);
		if (this.reward.getDiamonds() > 0) {
			y += 70;
			this.drawManager.drawDiamondBalance(this,
					this.reward.getDiamonds(), y);
			this.drawManager.drawCenteredRegularString(this,
					"bonus diamonds", y + 25, Color.WHITE);
		}

		this.drawManager.drawCenteredRegularString(this, "Banked!",
				y + 65, Color.GREEN);
		this.drawManager.drawCenteredRegularString(this, "You have "
				+ CurrencyManager.getInstance().getCoins() + " coins, "
				+ DiamondManager.getInstance().getDiamonds() + " diamonds",
				y + 85, Color.WHITE);

		this.drawManager.drawKeyHints(this, "space continue");
		this.drawManager.completeDrawing(this);
	}
}
