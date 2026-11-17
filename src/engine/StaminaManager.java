package engine;

/**
 * Owns the player's stamina, including run consumption and real-time
 * regeneration. Stamina is shared by Story Mode and Endless Mode.
 *
 * The current amount and its regeneration timestamp are persisted together,
 * allowing regeneration to continue while the game is closed.
 */
public final class StaminaManager {

    /** Maximum stamina a player can store. */
    public static final int MAX_STAMINA = 5;
    /** Stamina charged when a new run starts. */
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
        save();
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
     * Consumes stamina for a new Story or Endless run. Tester mode bypasses
     * consumption without changing the player's saved stamina.
     *
     * @return true when the run may start; false when stamina is insufficient.
     */
    public synchronized boolean tryConsumeForRun() {
        if (isTesterMode())
            return true;
        updateRegeneration(System.currentTimeMillis());
        if (this.stamina < RUN_COST)
            return false;
        if (this.stamina == MAX_STAMINA)
            this.lastUpdateTime = System.currentTimeMillis();
        this.stamina -= RUN_COST;
        save();
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
        if (!DiamondManager.getInstance().trySpend(1))
            return false;
        this.stamina = MAX_STAMINA;
        this.lastUpdateTime = System.currentTimeMillis();
        save();
        return true;
    }

    /**
     * @return true only when the development tester bypass was requested.
     */
    public boolean isTesterMode() {
        return Boolean.getBoolean(TESTER_MODE_PROPERTY);
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
        this.stamina = (int) result[0];
        this.lastUpdateTime = result[1];
        if (changed)
            save();
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
    private void save() {
        FileManager.getInstance().saveStamina(this.stamina,
                this.lastUpdateTime);
    }
}
