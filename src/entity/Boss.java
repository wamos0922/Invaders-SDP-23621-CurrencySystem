package entity;

import java.util.Set;
import engine.DrawManager;
import engine.GameSettings;

/** Behavior shared by the Stage 5 and Stage 10 encounters. */
public interface Boss {
    int getPositionX();
    int getPositionY();
    /** Current remaining health points. */
    int getHp();
    /** Health points this boss starts an encounter with. */
    int getMaxHp();
    boolean isDestroyed();
    void destroy();
    void update();
    void moveBoss(int screenWidth, int screenHeight, GameSettings settings);
    boolean shoot(Set<Bullet> bullets, GameSettings settings);
    int manageCollisionsBoss(Bullet bullet);

    /** Tells the boss where the player is, for aimed attacks. */
    default void setTarget(final int x, final int y) { }

    /** False while the boss should not be drawn (e.g. flickering). */
    default boolean isVisible() { return true; }

    /** Draws attack and movement warnings, such as incoming bullet rain. */
    default void drawWarnings(final DrawManager drawManager) { }

    /**
     * Text shown in the middle of the screen before the boss appears, or
     * null for none (GoG, GOG-75).
     *
     * @return Warning text, or null.
     */
    default String getWarningText() { return null; }

    /**
     * Keeps the boss's timers still while the game is paused.
     *
     * @param pausedMilliseconds How long the game was paused.
     */
    default void resumeAfterPause(final long pausedMilliseconds) { }

    /**
     * Enemies the boss called that the player can shoot, or an empty list
     * (GoG, GOG-75: the Tempest Queen's phase 2 mobs).
     *
     * @return The boss's minions.
     */
    default java.util.List<EnemyShip> getMinions() {
        return java.util.Collections.emptyList();
    }
}
