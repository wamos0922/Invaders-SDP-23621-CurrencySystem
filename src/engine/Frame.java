package engine;

import java.awt.Insets;

import javax.swing.JFrame;

import screen.Screen;

/**
 * Implements a frame to show screens on.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
@SuppressWarnings("serial")
public class Frame extends JFrame {

	/** Frame width. */
	private int width;
	/** Frame height. */
	private int height;
	/** Screen currently shown. */
	private Screen currentScreen;
	/** Window magnification (GOG-5). The game keeps drawing on its original
	 * logical canvas; DrawManager and InputManager scale to/from this. */
	private final double scale;

	/**
	 * Initializes the new frame.
	 * 
	 * @param width
	 *            Frame width.
	 * @param height
	 *            Frame height.
	 */
	public Frame(final int width, final int height) {
		this(width, height, 1.0);
	}

	/**
	 * Initializes the new frame, magnified by the given scale.
	 *
	 * @param width
	 *            Base (unscaled) frame width.
	 * @param height
	 *            Base (unscaled) frame height.
	 * @param scale
	 *            Window magnification, 1.0 = original size.
	 */
	public Frame(final int width, final int height, final double scale) {
		this.scale = scale;
		setSize(width, height);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setLocationRelativeTo(null);
		setVisible(true);

		Insets insets = getInsets();
		this.width = width - insets.left - insets.right;
		this.height = height - insets.top + insets.bottom;

		// Enlarge the window; screens still see the original logical size.
		// Size it from the canvas the screens draw on (this.width x
		// this.height), so the bottom rows are not cut off by the window
		// border (GOG-12).
		setSize((int) Math.round(this.width * scale) + insets.left + insets.right,
				(int) Math.round(this.height * scale) + insets.top + insets.bottom);
		setLocationRelativeTo(null);
		setTitle("Invaders");

		addKeyListener(Core.getInputManager());

		getContentPane().addMouseMotionListener(Core.getInputManager());
		getContentPane().addMouseListener(Core.getInputManager());

	}
	/**
	 * Sets current screen.
	 * 
	 * @param screen
	 *            Screen to show.
	 * @return Return code of the finished screen.
	 */
	public final int setScreen(final Screen screen) {
		currentScreen = screen;
		currentScreen.initialize();
		return currentScreen.run();
	}

	/**
	 * Getter for the window magnification.
	 *
	 * @return Scale factor, 1.0 = original size.
	 */
	public final double getScale() {
		return this.scale;
	}

	/**
	 * Getter for frame width.
	 * 
	 * @return Frame width.
	 */
	public final int getWidth() {
		return this.width;
	}

	/**
	 * Getter for frame height.
	 * 
	 * @return Frame height.
	 */

	public final int getHeight() {
		return this.height;
	}
}
