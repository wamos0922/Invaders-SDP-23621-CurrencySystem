package engine;

/**
 * Owns the player's stamina and real-time regeneration. Stamina is shared
 * by Story Mode and Endless Mode. Story Mode charges once per level attempt;
 * Endless Mode charges once per run.
 *
 * The current amount and its regeneration timestamp are persisted together,
 * allowing regeneration to continue while the game is closed.
 */
public final class StaminaManager {

    /** Maximum stamina a player can store. */
    public static final int MAX_STAMINA = 5;
    /** Stamina charged for each Story level attempt or each Endless run. */
    public static final int RUN_COST = 1;
    /** Milliseconds required to regenerate one stamina. */
    public static final long REGEN_INTERVAL_MILLIS = 10L * 60L * 1000L;
    /** System property used to enable development-only tester bypass. */
    public static final String TESTER_MODE_PROPERTY = "game.testerMode";

    private static StaminaManager instance;

    private int stamina;
    /** Start of the current regeneration interval, in epoch milliseconds. */
    private long lastUpdateTime;

    private StaminaManager() {
        long now = System.currentTimeMillis();
        this.stamina = Math.min(MAX_STAMINA,
                FileManager.getInstance().loadStamina(MAX_STAMINA));
        this.lastUpdateTime = FileManager.getInstance()
                .loadStaminaTimestamp(now);
        updateRegeneration(now);
    }

    /**
     * Returns the shared stamina manager.
     *
     * @return Singleton stamina manager.
     */
    public static StaminaManager getInstance() {
        if (instance == null)
            instance = new StaminaManager();
        return instance;
    }

    /**
     * @return Current stamina after applying any elapsed regeneration.
     */
    public synchronized int getStamina() {
        updateRegeneration(System.currentTimeMillis());
        return this.stamina;
    }

    /**
     * @return Maximum stamina capacity.
     */
    public int getMaxStamina() {
        return MAX_STAMINA;
    }

    /**
     * @return Milliseconds until the next stamina unit, or zero when full.
     */
    public synchronized long getMillisUntilNextStamina() {
        long now = System.currentTimeMillis();
        updateRegeneration(now);
        if (this.stamina >= MAX_STAMINA)
            return 0L;
        long elapsed = Math.max(0L, now - this.lastUpdateTime);
        return Math.max(0L, REGEN_INTERVAL_MILLIS - elapsed);
    }

    /**
     * Checks whether a run can be started. Tester mode bypasses this limit.
     *
     * @return true when enough stamina is available or tester mode is active.
     */
    public synchronized boolean canStartRun() {
        return isTesterMode() || getStamina() >= RUN_COST;
    }

    /**
     * Consumes stamina for one Story level attempt or one Endless run.
     * Story Mode calls this once before each level; Endless calls it once
     * before the run. Tester mode bypasses consumption without changing
     * the player's saved stamina.
     *
     * @return true when the run may start; false when stamina is insufficient.
     */
    public synchronized boolean tryConsumeForRun() {
        if (isTesterMode())
            return true;
        updateRegeneration(System.currentTimeMillis());
        if (this.stamina < RUN_COST)
            return false;
        long now = System.currentTimeMillis();
        long updatedTimestamp = this.stamina == MAX_STAMINA
                ? now : this.lastUpdateTime;
        int updatedStamina = this.stamina - RUN_COST;
        if (!save(updatedStamina, updatedTimestamp))
            return false;
        this.stamina = updatedStamina;
        this.lastUpdateTime = updatedTimestamp;
        return true;
    }

    /**
     * Refills stamina to maximum by spending one diamond. A full stamina
     * bar cannot be refilled. Tester mode does not make refills free.
     *
     * @return true if one diamond was spent and stamina was refilled.
     */
    public synchronized boolean tryRefillWithDiamond() {
        updateRegeneration(System.currentTimeMillis());
        if (this.stamina >= MAX_STAMINA)
            return false;
        long now = System.currentTimeMillis();
        if (!DiamondManager.getInstance().trySpendForStaminaRefill(
                MAX_STAMINA, now))
            return false;
        this.stamina = MAX_STAMINA;
        this.lastUpdateTime = now;
        return true;
    }

    /**
     * Sets the stamina value for local testing without waiting for gameplay.
     * This operation is available only when tester mode is enabled.
     *
     * @param value Stamina value to set; values are clamped to the valid range.
     * @throws IllegalStateException when tester mode is not enabled.
     */
    public synchronized void setStaminaForTester(final int value) {
        requireTesterMode();
        int updatedStamina = Math.max(0, Math.min(MAX_STAMINA, value));
        long now = System.currentTimeMillis();
        if (!save(updatedStamina, now))
            throw new IllegalStateException("Could not persist tester stamina");
        this.stamina = updatedStamina;
        this.lastUpdateTime = now;
    }

    /**
     * Advances the stamina clock for local testing without sleeping.
     * Any partial regeneration interval is preserved after the simulation.
     *
     * @param elapsedMillis Simulated elapsed time; must not be negative.
     * @throws IllegalStateException when tester mode is not enabled.
     * @throws IllegalArgumentException when elapsedMillis is negative.
     */
    public synchronized void simulateElapsedTimeForTester(
            final long elapsedMillis) {
        requireTesterMode();
        if (elapsedMillis < 0L)
            throw new IllegalArgumentException(
                    "elapsedMillis must not be negative");
        if (this.stamina >= MAX_STAMINA || elapsedMillis == 0L)
            return;
        long simulatedTimestamp = calculateSimulatedTimestamp(
                this.lastUpdateTime, elapsedMillis);
        long now = System.currentTimeMillis();
        long[] result = calculateRegeneration(this.stamina,
                simulatedTimestamp, now);
        int updatedStamina = (int) result[0];
        if (save(updatedStamina, result[1])) {
            this.stamina = updatedStamina;
            this.lastUpdateTime = result[1];
        }
    }

    /**
     * @return true only when the development tester bypass was requested.
     */
    public boolean isTesterMode() {
        return Boolean.getBoolean(TESTER_MODE_PROPERTY);
    }

    /** Ensures development-only controls cannot be used in normal play. */
    private void requireTesterMode() {
        if (!isTesterMode())
            throw new IllegalStateException(
                    "Enable -D" + TESTER_MODE_PROPERTY
                            + "=true to use tester controls");
    }

    /**
     * Applies complete regeneration intervals while preserving any partial
     * interval. Clock rollback is treated as zero elapsed time.
     *
     * @param now Current epoch time in milliseconds.
     */
    private void updateRegeneration(final long now) {
        long[] result = calculateRegeneration(this.stamina,
                this.lastUpdateTime, now);
        boolean changed = result[0] != this.stamina
                || result[1] != this.lastUpdateTime;
        if (changed && !save((int) result[0], result[1]))
            return;
        this.stamina = (int) result[0];
        this.lastUpdateTime = result[1];
    }

    /**
     * Moves a saved timestamp backwards to simulate elapsed real time.
     * Package-private so the arithmetic can be tested without sleeping.
     */
    static long calculateSimulatedTimestamp(final long timestamp,
            final long elapsedMillis) {
        if (elapsedMillis < 0L)
            throw new IllegalArgumentException(
                    "elapsedMillis must not be negative");
        return elapsedMillis > timestamp ? 0L : timestamp - elapsedMillis;
    }

    /**
     * Pure regeneration calculation, separated so elapsed-time edge cases
     * can be tested without waiting or touching the player's save file.
     * The returned array contains stamina at index 0 and timestamp at index 1.
     */
    static long[] calculateRegeneration(final int currentStamina,
            final long lastUpdate, final long now) {
        int safeStamina = Math.max(0,
                Math.min(MAX_STAMINA, currentStamina));
        if (now < lastUpdate)
            return new long[] {safeStamina, now};
        if (safeStamina >= MAX_STAMINA)
            return new long[] {MAX_STAMINA, lastUpdate};

        long elapsed = now - lastUpdate;
        long regenerated = elapsed / REGEN_INTERVAL_MILLIS;
        if (regenerated <= 0L)
            return new long[] {safeStamina, lastUpdate};

        int newStamina = (int) Math.min(MAX_STAMINA,
                safeStamina + regenerated);
        long newTimestamp = newStamina >= MAX_STAMINA
                ? now
                : lastUpdate + regenerated * REGEN_INTERVAL_MILLIS;
        return new long[] {newStamina, newTimestamp};
    }

    /** Persists stamina and its regeneration reference time together. */
    private boolean save(final int stamina, final long timestamp) {
        return FileManager.getInstance().saveStamina(stamina, timestamp);
    }

    static void resetInstanceForTests() {
        instance = null;
    }
}
