package skilltree;

/**
 * The Armor branch in a level (GOG-89): Quick Repair, Launch Shield, Last
 * Stand and Hull Plating. Plain logic with the time passed in, so it can be
 * tested without the game. One instance per level.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillArmor {

	/**
	 * Team decision for Hull Plating: false = the first hit of every level
	 * is blocked (design doc), true = only in boss levels.
	 */
	public static final boolean HULL_PLATING_BOSS_LEVELS_ONLY = false;
	/** Time the ship is out after losing a life (Ship's 1 s cooldown). */
	static final long RESPAWN_MILLIS = 1000;
	/** Quick Repair: extra invincible time after coming back. */
	static final long QUICK_REPAIR_MILLIS = 500;
	/** Launch Shield length once play starts. */
	static final long LAUNCH_SHIELD_MILLIS = 2000;
	/** Last Stand shield length. */
	static final long LAST_STAND_MILLIS = 3000;

	/** Effects of the unlocked nodes. */
	private final SkillEffects effects;
	/** True in a boss level. */
	private final boolean bossLevel;
	/** Shield on until this time (Launch Shield, Last Stand). */
	private long shieldUntil;
	/** Invincible until this time (Quick Repair). */
	private long invincibleUntil;
	/** True once Hull Plating blocked its hit this level. */
	private boolean platingUsed;
	/** True once Last Stand gave its shield this level. */
	private boolean lastStandUsed;

	/**
	 * @param effects
	 *            Effects of the ship's unlocked nodes.
	 * @param bossLevel
	 *            True if this level has a boss.
	 */
	public SkillArmor(final SkillEffects effects, final boolean bossLevel) {
		this.effects = effects;
		this.bossLevel = bossLevel;
	}

	/**
	 * Call once when the level begins.
	 *
	 * @param playStartsAt
	 *            Time the countdown ends and enemies can shoot.
	 */
	public void startLevel(final long playStartsAt) {
		if (this.effects.has(SkillTreeCatalog.LAUNCH_SHIELD))
			this.shieldUntil = playStartsAt + LAUNCH_SHIELD_MILLIS;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return True while a skill shield is on (to draw it).
	 */
		public boolean shieldActive(final long now) {
		return now < this.shieldUntil;
	}

	/**
	 * The game was paused: shields and invincibility last that much longer,
	 * so pausing doesn't use them up (like the freeze and the Stinger's
	 * 1-life period).
	 *
	 * @param pausedMillis
	 *            How long the game was paused.
	 */
	public void resumeAfterPause(final long pausedMillis) {
		if (pausedMillis <= 0)
			return;
		// An end time before the pause stays in the past, so an expired
		// shield never comes back.
		if (this.shieldUntil > 0)
			this.shieldUntil += pausedMillis;
		if (this.invincibleUntil > 0)
			this.invincibleUntil += pausedMillis;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return True while Quick Repair keeps the ship invincible.
	 */
	public boolean isInvincible(final long now) {
		return now < this.invincibleUntil;
	}

	/**
	 * Call when the ship would be hit.
	 *
	 * @param now
	 *            Current time.
	 * @return True if the hit is blocked: skill shield on, Quick Repair
	 *         time, or Hull Plating's one block this level.
	 */
	public boolean tryBlockHit(final long now) {
		if (shieldActive(now) || isInvincible(now))
			return true;
		if (!this.platingUsed
				&& this.effects.has(SkillTreeCatalog.HULL_PLATING)
				&& (this.bossLevel || !HULL_PLATING_BOSS_LEVELS_ONLY)) {
			this.platingUsed = true;
			return true;
		}
		return false;
	}

	/**
	 * Call after a life is lost.
	 *
	 * @param livesLeft
	 *            Lives after the hit.
	 * @param now
	 *            Current time.
	 */
	public void onLifeLost(final int livesLeft, final long now) {
		if (this.effects.has(SkillTreeCatalog.QUICK_REPAIR))
			this.invincibleUntil = now + RESPAWN_MILLIS + QUICK_REPAIR_MILLIS;
		if (livesLeft == 1 && !this.lastStandUsed
				&& this.effects.has(SkillTreeCatalog.LAST_STAND)) {
			this.lastStandUsed = true;
			this.shieldUntil = Math.max(this.shieldUntil,
					now + LAST_STAND_MILLIS);
		}
	}
}
