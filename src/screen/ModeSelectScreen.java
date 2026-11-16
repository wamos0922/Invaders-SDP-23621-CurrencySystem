package screen;

import java.awt.event.KeyEvent;

import engine.Core;
import engine.Cooldown;
import engine.soundEngine.MenuSounds;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

/**
 * In-game mode selection menu using the existing title screen's
 * starfield, fonts, highlighting and background music.
 */
public class ModeSelectScreen extends Screen {

    public static final int STORY_SELECTED = 8;
    public static final int ENDLESS_SELECTED = 9;
    public static final int BACK_SELECTED = 1;

    private static final String[] OPTIONS = {
            "Story Mode", "Endless Mode", "Back"
    };

    private static final int SELECTION_TIME = 200;
    private static final int ROW_SPACING = 35;

    private final Cooldown selectionCooldown;
    private final Starfield starfield;

    private int selected = 0;
    private boolean confirmWasDown;
    private boolean escapeWasDown;
    private boolean moveSoundReady = true;

    public ModeSelectScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        selectionCooldown = Core.getCooldown(SELECTION_TIME);
        selectionCooldown.reset();
        starfield = TitleScreen.getSharedStarfield(width, height);

        confirmWasDown = inputManager.isKeyDown(KeyEvent.VK_ENTER)
                || inputManager.isKeyDown(KeyEvent.VK_SPACE);
        escapeWasDown = inputManager.isKeyDown(KeyEvent.VK_ESCAPE);

        // The same title music continues from the main menu.
        SoundManager.getInstance().playBgm(Sound.BGM_TITLE);
    }

    @Override
    public int run() {
        super.run();
        return returnCode;
    }

    @Override
    protected void update() {
        super.update();

        boolean confirmDown = inputManager.isKeyDown(KeyEvent.VK_ENTER)
                || inputManager.isKeyDown(KeyEvent.VK_SPACE);
        boolean confirmPressed = confirmDown && !confirmWasDown;
        confirmWasDown = confirmDown;

        boolean escapeDown = inputManager.isKeyDown(KeyEvent.VK_ESCAPE);
        boolean escapePressed = escapeDown && !escapeWasDown;
        escapeWasDown = escapeDown;

        starfield.update();
        draw();

        if (!inputDelay.checkFinished()) {
            return;
        }

        if (escapePressed) {
            returnCode = BACK_SELECTED;
            isRunning = false;
            return;
        }

        boolean up = inputManager.isKeyDown(KeyEvent.VK_UP)
                || inputManager.isKeyDown(KeyEvent.VK_W);
        boolean down = inputManager.isKeyDown(KeyEvent.VK_DOWN)
                || inputManager.isKeyDown(KeyEvent.VK_S);

        if (!up && !down) {
            moveSoundReady = true;
        }

        if (selectionCooldown.checkFinished() && up != down) {
            if (up) {
                selected = (selected + OPTIONS.length - 1) % OPTIONS.length;
            } else {
                selected = (selected + 1) % OPTIONS.length;
            }

            if (moveSoundReady) {
                MenuSounds.playMove();
                moveSoundReady = false;
            }
            selectionCooldown.reset();
        }

        if (inputManager.isMouseMoved()) {
            int hovered = optionAt(inputManager.getMouseY());
            if (hovered >= 0 && hovered != selected) {
                selected = hovered;
                MenuSounds.playMove();
            }
        }

        if (confirmPressed) {
            confirm();
            return;
        }

        if (inputManager.isMousePressed()) {
            int clicked = optionAt(inputManager.getMouseY());
            if (clicked >= 0) {
                selected = clicked;
                confirm();
            }
        }
    }

    private int optionBaseline(final int index) {
        return height / 2 + 70 + index * ROW_SPACING;
    }

    private int optionAt(final int mouseY) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (drawManager.isOnMenuRow(optionBaseline(i),
                    ROW_SPACING, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private void confirm() {
        if (selected == 0) {
            returnCode = STORY_SELECTED;
        } else if (selected == 1) {
            returnCode = ENDLESS_SELECTED;
        } else {
            returnCode = BACK_SELECTED;
        }

        if (returnCode != BACK_SELECTED) {
            SoundManager.getInstance().stopBgm();
        }
        isRunning = false;
    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawStarfield(this, starfield);
        drawManager.drawTitle(this);

        for (int i = 0; i < OPTIONS.length; i++) {
            drawManager.drawMenuRow(this, OPTIONS[i],
                    optionBaseline(i), selected == i);
        }

        drawManager.drawKeyHints(this,
                "w+s/arrows move, space select, esc back");
        drawManager.completeDrawing(this);
    }
}
