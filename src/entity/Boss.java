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
     * Called once, when the level's start countdown ends and play begins
     * (GoG, GOG-75). Bosses with timers start them here, not in the
     * constructor, so nothing runs out during the countdown.
     */
    default void startFight() { }

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

    /**
     * Hits the boss landed on the player since the last call, other than
     * bullets (GoG, GOG-77: the AI Commander's laser and claw). GameScreen
     * calls it every frame and takes a life (or the shield) for each.
     *
     * @param ship The player ship.
     * @return Number of hits, usually 0.
     */
    default int takePlayerHits(final Entity ship) { return 0; }

    /**
     * Called when the player's shield blocked one of the hits from
     * {@link #takePlayerHits(Entity)} (GoG, GOG-77).
     */
    default void onPlayerHitBlocked() { }

    /**
     * Extra screen shake the boss wants this frame, added to the game's own
     * shake (GoG, GOG-77), or null for none.
     *
     * @return Offset in pixels, or null.
     */
    default java.awt.Point getShakeOffset() { return null; }

    /**
     * Draws on top of everything, without screen shake (GoG, GOG-77: the
     * claw's screen flash and "SHIELD BROKEN").
     *
     * @param drawManager Draw manager.
     */
    default void drawOverlay(final DrawManager drawManager) { }
}
