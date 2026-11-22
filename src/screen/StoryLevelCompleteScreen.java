package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;

import engine.Core;
import engine.DiamondManager;

/**
 * Shown after EVERY successfully cleared Story Mode level.
 * This screen does not award diamonds: Core has already banked them and
 * saved the new checkpoint before the player can choose Continue or Exit.
 *
 * Continue -> next Story Mode level (or results after the final level).
 * Exit     -> title/main menu, keeping the saved next-level checkpoint.
 * Endless Mode does not use this class.
 */
public final class StoryLevelCompleteScreen extends Screen {

    private static final int INPUT_DELAY_MS = 500;
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = Color.GREEN;
    private static final Color GRAY = new Color(170, 170, 180);

    private final int clearedLevel;
    private final int diamondsEarned;
    private boolean selectedExit;
    private boolean didExit;

    // Wait until keys held while shooting have been released. Afterwards,
    // react only to new presses (not every frame of a held key).
    private boolean controlsReleased;
    private boolean lastMoveDown;
    private boolean lastConfirmDown;
    private boolean lastEscapeDown;
    private boolean lastMouseDown;

    public StoryLevelCompleteScreen(int width, int height, int fps,
                                    int clearedLevel, int diamondsEarned) {
        super(width, height, fps);
        this.clearedLevel = clearedLevel;
        this.diamondsEarned = Math.max(0, diamondsEarned);
        this.inputDelay = Core.getCooldown(INPUT_DELAY_MS);
        this.inputDelay.reset();
    }

    @Override
    public int run() {
        super.run();
        return this.returnCode;
    }

    /** True if the player chose Exit instead of Continue. */
    public boolean didExit() {
        return didExit;
    }

    @Override
    protected void update() {
        super.update();
        draw();

        boolean up = inputManager.isKeyDown(KeyEvent.VK_UP)
                || inputManager.isKeyDown(KeyEvent.VK_W);
        boolean down = inputManager.isKeyDown(KeyEvent.VK_DOWN)
                || inputManager.isKeyDown(KeyEvent.VK_S);
        boolean move = up || down;
        boolean confirm = inputManager.isKeyDown(KeyEvent.VK_SPACE)
                || inputManager.isKeyDown(KeyEvent.VK_ENTER);
        boolean escape = inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
        boolean mouse = inputManager.isMousePressed();

        if (!controlsReleased) {
            if (!move && !confirm && !escape && !mouse) {
                controlsReleased = true;
            }
            lastMoveDown = move;
            lastConfirmDown = confirm;
            lastEscapeDown = escape;
            lastMouseDown = mouse;
            return;
        }

        boolean pressedMove = move && !lastMoveDown;
        boolean pressedConfirm = confirm && !lastConfirmDown;
        boolean pressedEscape = escape && !lastEscapeDown;
        boolean pressedMouse = mouse && !lastMouseDown;

        lastMoveDown = move;
        lastConfirmDown = confirm;
        lastEscapeDown = escape;
        lastMouseDown = mouse;

        if (!this.inputDelay.checkFinished()) {
            return;
        }

        if (pressedMove) {
            if (up && !down) selectedExit = false;
            else if (down && !up) selectedExit = true;
        }

        if (pressedEscape) {
            didExit = true;
            isRunning = false;
        } else if (pressedConfirm) {
            didExit = selectedExit;
            isRunning = false;
        } else if (pressedMouse) {
            int y = inputManager.getMouseY();
            int continueY = this.height - 158;
            int exitY = this.height - 110;
            if (Math.abs(y - continueY) <= 22) {
                selectedExit = false;
                didExit = false;
                isRunning = false;
            } else if (Math.abs(y - exitY) <= 22) {
                selectedExit = true;
                didExit = true;
                isRunning = false;
            }
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawScreenTitle(this,
                "LEVEL " + clearedLevel + " COMPLETED!");

        drawManager.drawCenteredRegularString(this,
                "LEVEL CLEAR REWARD", this.height / 4, GRAY);
        drawManager.drawDiamondBalance(this,
                diamondsEarned, this.height / 3);
        drawManager.drawCenteredRegularString(this,
                "+" + diamondsEarned + (diamondsEarned == 1
                        ? " diamond earned" : " diamonds earned"),
                this.height / 3 + 32, WHITE);
        drawManager.drawCenteredRegularString(this,
                "Banked! Total: " + DiamondManager.getInstance().getDiamonds(),
                this.height / 3 + 64, GREEN);
        drawManager.drawCenteredRegularString(this,
                "Story progress saved", this.height / 3 + 93, GRAY);

        int continueY = this.height - 158;
        int exitY = this.height - 110;
        drawManager.drawCenteredRegularString(this,
                selectedExit ? "CONTINUE" : "> CONTINUE <",
                continueY, selectedExit ? WHITE : GREEN);
        drawManager.drawCenteredRegularString(this,
                selectedExit ? "> EXIT <" : "EXIT",
                exitY, selectedExit ? GREEN : WHITE);

        drawManager.drawKeyHints(this,
                "arrows move | space select | esc exit");
        drawManager.completeDrawing(this);
    }
}
