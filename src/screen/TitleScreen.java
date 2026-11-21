package screen;

import java.awt.event.KeyEvent;

import engine.Cooldown;
import engine.Core;
import engine.DiamondManager;
import engine.StaminaManager;
import engine.soundEngine.MenuSounds;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

/**
 * Implements the title screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
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
	/** Stars behind the menu, kept between visits so they don't jump. */
	private static Starfield starfield;
	/** Whether Space or Enter was held during the previous update. */
	private boolean confirmWasDown;
	/** Whether R was held during the previous update. */
	private boolean refillWasDown;
	/** Whether the stamina information popup is open. */
	private boolean showingStaminaInfo;
	/** Whether the stamina refill confirmation is open. */
	private boolean showingStaminaRefillConfirm;
	/** Whether Yes is selected in the stamina refill confirmation. */
	private boolean staminaRefillYes;
	/** Temporary feedback shown below the stamina panel. */
	private String staminaNotice = "";
	/** Time at which the temporary feedback should disappear. */
	private long staminaNoticeUntil;
	/** Prevents a held dialog click from activating the menu behind it. */
	private boolean ignoreMouseUntilRelease;
    /** Whether the next cursor move by key should make a sound. */
    private boolean moveSoundReady = true;

	/**
	 * Constructor, establishes the properties of the screen.
	 * 
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public TitleScreen(final int width, final int height, final int fps) {
		super(width, height, fps);

		// Starts where the cursor was when the menu last closed.
		this.selected = lastSelected;
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();
		this.escapeWasDown = this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
		this.confirmWasDown =
				(this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| this.inputManager.isKeyDown(KeyEvent.VK_ENTER));
		this.refillWasDown = this.inputManager.isKeyDown(KeyEvent.VK_R);
		if (starfield == null)
			starfield = new Starfield(width, height);

		// Title music; does nothing if it is already playing.
		SoundManager.getInstance().playBgm(Sound.BGM_TITLE);
	}

	/**
	 * Starts the action.
	 * 
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		final boolean escapeDown =
				this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
		final boolean escapePressed = escapeDown && !this.escapeWasDown;
		this.escapeWasDown = escapeDown;

		final boolean confirmDown =
				this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
				|| this.inputManager.isKeyDown(KeyEvent.VK_ENTER);
		final boolean confirmPressed = confirmDown && !this.confirmWasDown;
		this.confirmWasDown = confirmDown;
		final boolean refillDown = this.inputManager.isKeyDown(KeyEvent.VK_R);
		final boolean refillPressed = refillDown && !this.refillWasDown;
		this.refillWasDown = refillDown;

		starfield.update();
		draw();

		if (!this.inputDelay.checkFinished()) {
			return;
		}

		if (escapePressed) {
			if (this.showingStaminaInfo) {
				this.showingStaminaInfo = false;
			} else if (this.showingStaminaRefillConfirm) {
				cancelStaminaRefill();
			} else if (this.showingExitConfirm) {
				closeExitConfirm();
			} else {
				openExitConfirm();
			}
			return;
		}

		if (this.showingStaminaInfo) {
			if (confirmPressed || inputManager.isMousePressed())
				this.showingStaminaInfo = false;
		} else if (this.showingStaminaRefillConfirm) {
			updateStaminaRefillConfirm(confirmPressed);
		} else if (this.showingExitConfirm) {
			updateExitConfirm(confirmPressed);
		} else {
			updateMenu(confirmPressed, refillPressed);
		}

		if (inputManager.isMouseMoved()) {
			if (this.showingStaminaInfo) {
				// The information popup intentionally has no hover actions.
			} else if (this.showingStaminaRefillConfirm) {
				final Boolean answer = drawManager.staminaRefillAnswerAt(this,
						inputManager.getMouseX(), inputManager.getMouseY());
				if (answer != null && answer.booleanValue() != this.staminaRefillYes) {
					this.staminaRefillYes = answer;
					MenuSounds.playMove();
				}
			} else if (this.showingExitConfirm) {
				final Boolean answer = drawManager.exitConfirmAnswerAt(this,
						inputManager.getMouseX(), inputManager.getMouseY());
				if (answer != null && answer.booleanValue() != this.exitConfirmYes) {
					this.exitConfirmYes = answer;
					MenuSounds.playMove();
				}
			} else {
				MenuItem item = drawManager.menuItemAt(this,
						inputManager.getMouseY());
				if (item != null && item != this.selected) {
					this.selected = item;
					MenuSounds.playMove();
				}
			}
		}

	}

	/**
	 * Handles input while the menu has focus.
	 */
	private void updateMenu(final boolean confirmPressed,
			final boolean refillPressed) {
        // Re-arm the move sound once every navigation key is released.
        if (!inputManager.isKeyDown(KeyEvent.VK_UP)
                && !inputManager.isKeyDown(KeyEvent.VK_W)
                && !inputManager.isKeyDown(KeyEvent.VK_DOWN)
                && !inputManager.isKeyDown(KeyEvent.VK_S))
            this.moveSoundReady = true;

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
		if (refillPressed)
			openStaminaRefillConfirm();
		if (confirmPressed)
			confirm();
		if (this.ignoreMouseUntilRelease) {
			if (!inputManager.isMousePressed())
				this.ignoreMouseUntilRelease = false;
			else
				return;
		}
		if (inputManager.isMousePressed()) {
			if (drawManager.isStaminaInfoAt(this,
					inputManager.getMouseX(), inputManager.getMouseY())) {
				this.showingStaminaInfo = true;
				return;
			}
			if (drawManager.isStaminaRefillAt(this,
					inputManager.getMouseX(), inputManager.getMouseY())) {
				openStaminaRefillConfirm();
				return;
			}
			final MenuItem item = drawManager.menuItemAt(this,
					inputManager.getMouseY());
			if (item != null) {
				this.selected = item;
				confirm();
			}
		}
	}

    /** Plays the move sound once per key press, not while the key is held. */
    private void playKeyMoveSound() {
        if (this.moveSoundReady) {
            MenuSounds.playMove();
            this.moveSoundReady = false;
        }
    }

	/** Opens the diamond refill confirmation, or explains why it is unavailable. */
	private void openStaminaRefillConfirm() {
		StaminaManager stamina = StaminaManager.getInstance();
		if (stamina.getStamina() >= stamina.getMaxStamina()) {
			showStaminaNotice("Stamina is already full.");
			return;
		}
		this.showingStaminaRefillConfirm = true;
		this.staminaRefillYes = false;
		this.selectionCooldown.reset();
	}

	/** Handles keyboard and mouse input while the refill confirmation is open. */
	private void updateStaminaRefillConfirm(final boolean confirmPressed) {
		if (this.selectionCooldown.checkFinished()) {
			boolean yes = inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| inputManager.isKeyDown(KeyEvent.VK_A)
					|| inputManager.isKeyDown(KeyEvent.VK_Y);
			boolean no = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| inputManager.isKeyDown(KeyEvent.VK_D)
					|| inputManager.isKeyDown(KeyEvent.VK_N);
			if (yes != no && this.staminaRefillYes != yes) {
				this.staminaRefillYes = yes;
				MenuSounds.playMove();
				this.selectionCooldown.reset();
			}
		}
		if (confirmPressed) {
			if (this.staminaRefillYes)
				confirmStaminaRefill();
			else
				cancelStaminaRefill();
		}
		if (inputManager.isMousePressed()) {
			Boolean answer = drawManager.staminaRefillAnswerAt(this,
					inputManager.getMouseX(), inputManager.getMouseY());
			if (answer != null) {
				this.staminaRefillYes = answer;
				this.ignoreMouseUntilRelease = true;
				if (answer)
					confirmStaminaRefill();
				else
					cancelStaminaRefill();
			}
		}
	}

	/** Uses the existing atomic stamina/diamond transaction for the refill. */
	private void confirmStaminaRefill() {
		this.showingStaminaRefillConfirm = false;
		if (StaminaManager.getInstance().tryRefillWithDiamond())
			showStaminaNotice("Stamina refilled to 5/5.");
		else if (StaminaManager.getInstance().getStamina()
				>= StaminaManager.getInstance().getMaxStamina())
			showStaminaNotice("Stamina is already full.");
		else if (DiamondManager.getInstance().getDiamonds() < 1)
			showStaminaNotice("Not enough diamonds to refill.");
		else
			showStaminaNotice("Refill failed - resources unchanged.");
		this.selectionCooldown.reset();
	}

	private void cancelStaminaRefill() {
		this.showingStaminaRefillConfirm = false;
		this.selectionCooldown.reset();
	}

	private void showStaminaNotice(final String notice) {
		this.staminaNotice = notice;
		this.staminaNoticeUntil = System.currentTimeMillis() + 3000L;
	}

	/** Formats a positive duration as a player-friendly mm:ss countdown. */
	static String formatStaminaCountdown(final long milliseconds) {
		long seconds = Math.max(0L, (milliseconds + 999L) / 1000L);
		long minutes = seconds / 60L;
		long remainder = seconds % 60L;
		return String.format(java.util.Locale.ROOT, "%02d:%02d",
				minutes, remainder);
	}

	/**
	 * Handles input while the exit confirmation is open. The cursor starts on
	 * No, so a second Space or Enter press cannot close the game by accident.
	 * Yes sits left of No, so left and A choose Yes while right and D choose
	 * No, the way the arrows move the cursor in the menu. Pressing towards
	 * the answer already chosen leaves it where it is.
	 */
	private void updateExitConfirm(final boolean confirmPressed) {
		if (this.selectionCooldown.checkFinished()) {
			final boolean yesKeyDown = inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| inputManager.isKeyDown(KeyEvent.VK_A);
			final boolean noKeyDown = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| inputManager.isKeyDown(KeyEvent.VK_D);

			// One side at a time, and only when it changes the answer, so
			// both sides held cancel out and pressing the side already
			// chosen does nothing.
			if (yesKeyDown != noKeyDown
					&& this.exitConfirmYes != yesKeyDown) {
				this.exitConfirmYes = yesKeyDown;
                MenuSounds.playMove();
				this.selectionCooldown.reset();
			}
		}
		if (confirmPressed)
			answerExitConfirm();

		if (inputManager.isMousePressed()) {
			final Boolean answer = drawManager.exitConfirmAnswerAt(this,
					inputManager.getMouseX(), inputManager.getMouseY());
			if (answer != null) {
				this.exitConfirmYes = answer;
				answerExitConfirm();
			}
		}
	}

	/**
	 * Acts on the answer the cursor is on. Every way of answering - Space,
	 * Enter or a mouse click - goes through here.
	 */
	private void answerExitConfirm() {
		if (this.exitConfirmYes) {
			this.returnCode = MenuItem.EXIT.getCode();
			this.isRunning = false;
		} else
			closeExitConfirm();
	}

	/**
	 * Closes the confirmation and hands focus back to the menu.
	 */
	private void closeExitConfirm() {
		this.showingExitConfirm = false;
		this.exitConfirmYes = false;
		this.selectionCooldown.reset();
	}

	/**
	 * Keys the player can use right now.
	 *
	 * @return Line drawn at the bottom of the screen.
	 */
	private String keyHints() {
		if (this.showingStaminaInfo)
			return "click anywhere or esc close";
		if (this.showingExitConfirm || this.showingStaminaRefillConfirm)
			return "arrows to choose, space ok, esc cancel";
		if (System.currentTimeMillis() < this.staminaNoticeUntil)
			return this.staminaNotice;

		return "click energy info | R refill | arrows move | space select";
	}

	/**
	 * Chooses the item the cursor is on and closes the screen, so Core opens
	 * the screen behind it. Every way of choosing an item - Space, Enter or a
	 * mouse click - goes through here. Disabled items are ignored, and Exit
	 * opens the confirmation instead of closing the game, so every way of
	 * choosing Exit is confirmed.
	 */
	private void confirm() {
		if (!this.selected.isEnabled())
			return;

		if (this.selected == MenuItem.EXIT) {
			openExitConfirm();
			return;
		}

		// Give immediate feedback before Core's backend safety check.
		if ((this.selected == MenuItem.MAIN_STORY
				|| this.selected == MenuItem.ENDLESS)
				&& !StaminaManager.getInstance().canStartRun()) {
			showStaminaNotice("Stamina empty - refill or wait.");
			return;
		}

		// The title music only belongs to the menu, so stop it when a game starts.
		if (this.selected == MenuItem.MAIN_STORY
				|| this.selected == MenuItem.ENDLESS) {
			SoundManager.getInstance().stopBgm();
		}

		lastSelected = this.selected;
		this.returnCode = this.selected.getCode();
		this.isRunning = false;
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawStarfield(this, starfield);
		drawManager.drawTitle(this);
		StaminaManager stamina = StaminaManager.getInstance();
		long remaining = stamina.getMillisUntilNextStamina();
		String countdown = formatStaminaCountdown(remaining);
		drawManager.drawStaminaPanel(this, stamina.getStamina(),
				stamina.getMaxStamina(), DiamondManager.getInstance().getDiamonds());
		drawManager.drawMenu(this, this.selected);
		drawManager.drawKeyHints(this, keyHints());

		// Drawn last so dialogs sit above the title menu.
		if (this.showingExitConfirm)
			drawManager.drawExitConfirm(this, this.exitConfirmYes);
		else if (this.showingStaminaRefillConfirm)
			drawManager.drawStaminaRefillConfirm(this, this.staminaRefillYes,
					DiamondManager.getInstance().getDiamonds());
		else if (this.showingStaminaInfo)
			drawManager.drawStaminaInfo(this, stamina.getStamina(),
					stamina.getMaxStamina(), countdown, remaining,
					StaminaManager.REGEN_INTERVAL_MILLIS);

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