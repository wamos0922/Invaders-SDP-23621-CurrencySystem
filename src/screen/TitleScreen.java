
package screen;

import java.awt.event.KeyEvent;

import engine.Cooldown;
import engine.Core;
import engine.soundEngine.MenuSounds;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

public class TitleScreen extends Screen {

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;

	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;

	/** Menu item the cursor is on. */
	private MenuItem selected;

	/** Item the cursor was on when the menu last closed. */
	private static MenuItem lastSelected = MenuItem.first();

	/** Whether the exit confirmation is open. */
	private boolean showingExitConfirm;

	/** Whether the cursor in the confirmation is on Yes. */
	private boolean exitConfirmYes;

	/** Whether Escape was held during the previous update. */
	private boolean escapeWasDown;

	/** Stars behind the menu, shared with the mode selection screen. */
	private static Starfield starfield;

	/** Whether Space or Enter was held during the previous update. */
	private boolean confirmWasDown;

	/** Whether the next cursor move should make a sound. */
	private boolean moveSoundReady = true;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width Screen width.
	 * @param height Screen height.
	 * @param fps Frames per second.
	 */
	public TitleScreen(
			final int width,
			final int height,
			final int fps) {

		super(width, height, fps);

		// Starts where the cursor was when the menu last closed.
		this.selected = lastSelected;

		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();

		this.escapeWasDown =
				this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE);

		this.confirmWasDown =
				this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
						|| this.inputManager.isKeyDown(KeyEvent.VK_ENTER);

		// Reuse the existing animated background.
		getSharedStarfield(width, height);

		// Keep title music playing across both menus.
		SoundManager.getInstance().playBgm(Sound.BGM_TITLE);
	}

	/**
	 * Returns the shared animated starfield.
	 *
	 * Both TitleScreen and ModeSelectScreen use this
	 * instance to preserve the same background.
	 *
	 * @param width Screen width.
	 * @param height Screen height.
	 * @return Shared Starfield instance.
	 */
	public static Starfield getSharedStarfield(
			final int width,
			final int height) {

		if (starfield == null) {
			starfield = new Starfield(width, height);
		}

		return starfield;
	}

	/**
	 * Starts the title screen.
	 *
	 * @return Next screen code.
	 */
	@Override
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	@Override
	protected final void update() {
		super.update();

		final boolean escapeDown =
				this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE);

		final boolean escapePressed =
				escapeDown && !this.escapeWasDown;

		this.escapeWasDown = escapeDown;

		final boolean confirmDown =
				this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
						|| this.inputManager.isKeyDown(KeyEvent.VK_ENTER);

		final boolean confirmPressed =
				confirmDown && !this.confirmWasDown;

		this.confirmWasDown = confirmDown;

		// Keep the animated background moving.
		starfield.update();

		draw();

		if (!this.inputDelay.checkFinished()) {
			return;
		}

		if (escapePressed) {
			if (this.showingExitConfirm) {
				closeExitConfirm();
			} else {
				openExitConfirm();
			}
			return;
		}

		if (this.showingExitConfirm) {
			updateExitConfirm(confirmPressed);
		} else {
			updateMenu(confirmPressed);
		}

		if (inputManager.isMouseMoved()) {

			if (this.showingExitConfirm) {

				final Boolean answer =
						drawManager.exitConfirmAnswerAt(
								this,
								inputManager.getMouseX(),
								inputManager.getMouseY()
						);

				if (answer != null
						&& answer.booleanValue() != this.exitConfirmYes) {

					this.exitConfirmYes = answer;
					MenuSounds.playMove();
				}

			} else {

				MenuItem item = drawManager.menuItemAt(
						this,
						inputManager.getMouseY()
				);

				if (item != null && item != this.selected) {
					this.selected = item;
					MenuSounds.playMove();
				}
			}
		}
	}

	/**
	 * Handles input while the main menu has focus.
	 */
	private void updateMenu(final boolean confirmPressed) {

		// Re-arm the movement sound when navigation keys are released.
		if (!inputManager.isKeyDown(KeyEvent.VK_UP)
				&& !inputManager.isKeyDown(KeyEvent.VK_W)
				&& !inputManager.isKeyDown(KeyEvent.VK_DOWN)
				&& !inputManager.isKeyDown(KeyEvent.VK_S)) {

			this.moveSoundReady = true;
		}

		if (this.selectionCooldown.checkFinished()) {

			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {

				this.selected = this.selected.previous();

				playKeyMoveSound();

				this.selectionCooldown.reset();
			}

			if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {

				this.selected = this.selected.next();

				playKeyMoveSound();

				this.selectionCooldown.reset();
			}
		}

		if (confirmPressed) {
			confirm();
		}

		if (inputManager.isMousePressed()) {

			final MenuItem item = drawManager.menuItemAt(
					this,
					inputManager.getMouseY()
			);

			if (item != null) {
				this.selected = item;
				confirm();
			}
		}
	}

	/**
	 * Plays the movement sound once per key press.
	 */
	private void playKeyMoveSound() {

		if (this.moveSoundReady) {
			MenuSounds.playMove();
			this.moveSoundReady = false;
		}
	}

	/**
	 * Handles input while the exit confirmation is open.
	 *
	 * The cursor starts on No.
	 */
	private void updateExitConfirm(
			final boolean confirmPressed) {

		if (this.selectionCooldown.checkFinished()) {

			final boolean yesKeyDown =
					inputManager.isKeyDown(KeyEvent.VK_LEFT)
							|| inputManager.isKeyDown(KeyEvent.VK_A);

			final boolean noKeyDown =
					inputManager.isKeyDown(KeyEvent.VK_RIGHT)
							|| inputManager.isKeyDown(KeyEvent.VK_D);

			if (yesKeyDown != noKeyDown
					&& this.exitConfirmYes != yesKeyDown) {

				this.exitConfirmYes = yesKeyDown;

				MenuSounds.playMove();

				this.selectionCooldown.reset();
			}
		}

		if (confirmPressed) {
			answerExitConfirm();
		}

		if (inputManager.isMousePressed()) {

			final Boolean answer =
					drawManager.exitConfirmAnswerAt(
							this,
							inputManager.getMouseX(),
							inputManager.getMouseY()
					);

			if (answer != null) {
				this.exitConfirmYes = answer;
				answerExitConfirm();
			}
		}
	}

	/**
	 * Acts on the selected exit confirmation answer.
	 */
	private void answerExitConfirm() {

		if (this.exitConfirmYes) {

			this.returnCode = MenuItem.EXIT.getCode();
			this.isRunning = false;

		} else {

			closeExitConfirm();
		}
	}

	/**
	 * Closes the exit confirmation.
	 */
	private void closeExitConfirm() {

		this.showingExitConfirm = false;
		this.exitConfirmYes = false;

		this.selectionCooldown.reset();
	}

	/**
	 * Returns the control hints displayed at the bottom.
	 *
	 * @return Control hints.
	 */
	private String keyHints() {

		if (this.showingExitConfirm) {
			return "arrows to choose, space ok, esc cancel";
		}

		return "w+s/arrows move, space select, esc exit";
	}


	private void confirm() {

		if (!this.selected.isEnabled()) {
			return;
		}

		if (this.selected == MenuItem.EXIT) {
			openExitConfirm();
			return;
		}

		// Remember the last selected menu item.
		lastSelected = this.selected;

		// PLAY returns code 2 to Core.java.
		// Core then opens ModeSelectScreen.
		//
		// Do not stop the background music here.
		// ModeSelectScreen stops it when a mode is selected.

		this.returnCode = this.selected.getCode();
		this.isRunning = false;
	}

	/**
	 * Draws the title screen.
	 */
	private void draw() {

		drawManager.initDrawing(this);

		// Animated background.
		drawManager.drawStarfield(this, starfield);

		// Original title and font.
		drawManager.drawTitle(this);

		// Original menu.
		drawManager.drawMenu(this, this.selected);

		// Original control hints.
		drawManager.drawKeyHints(this, keyHints());

		// Exit confirmation appears above the menu.
		if (this.showingExitConfirm) {
			drawManager.drawExitConfirm(
					this,
					this.exitConfirmYes
			);
		}

		drawManager.completeDrawing(this);
	}

	/**
	 * Opens the exit confirmation with No selected.
	 */
	private void openExitConfirm() {

		this.showingExitConfirm = true;
		this.exitConfirmYes = false;

		this.selectionCooldown.reset();
	}
}
