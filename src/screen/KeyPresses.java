package screen;

import engine.InputManager;

/**
 * Turns held keys and mouse button into single presses, so holding a key
 * acts once. Used by the skill tree, items and converter screens (GOG-51).
 * Keys and clicks still held from the previous screen are ignored.
 *
 * @author GoG - Progression (GOG-70)
 */
final class KeyPresses {
	/** Where the key and mouse states come from. */
	private final InputManager input;
	/** Keys watched. */
	private final int[] keys;
	/** Key states last frame. Null until the first frame. */
	private boolean[] wasDown;
	/** Mouse state last frame. */
	private boolean mouseWasDown;
	/** True if the mouse button went down this frame. */
	private boolean clicked;

	/**
	 * @param input
	 *            Input manager of the game.
	 * @param keys
	 *            Key codes to watch (KeyEvent.VK_...).
	 */
	KeyPresses(final InputManager input, final int... keys) {
		this.input = input;
		this.keys = keys.clone();
	}

	/**
	 * Call once per frame, after reading presses for this frame.
	 * On the first call it only records what is already held.
	 */
	void endFrame() {
		if (this.wasDown == null)
			this.wasDown = new boolean[this.keys.length];
		for (int i = 0; i < this.keys.length; i++)
			this.wasDown[i] = this.input.isKeyDown(this.keys[i]);
		this.mouseWasDown = this.input.isMousePressed();
	}

	/** Call once per frame, before reading presses. */
	void startFrame() {
		if (this.wasDown == null) {
			// First frame: what is held now came from the last screen.
			endFrame();
			this.clicked = false;
			return;
		}
		boolean down = this.input.isMousePressed();
		this.clicked = down && !this.mouseWasDown;
	}

	/**
	 * @param key
	 *            Key code; must be one of the watched keys.
	 * @return True if the key went down this frame.
	 */
	boolean pressed(final int key) {
		for (int i = 0; i < this.keys.length; i++)
			if (this.keys[i] == key)
				return this.input.isKeyDown(key) && !this.wasDown[i];
		return false;
	}

	/** @return True if the mouse button went down this frame. */
	boolean clicked() {
		return this.clicked;
	}

	/** @return Mouse x on the screen. */
	int mouseX() {
		return this.input.getMouseX();
	}

	/** @return Mouse y on the screen. */
	int mouseY() {
		return this.input.getMouseY();
	}
}
