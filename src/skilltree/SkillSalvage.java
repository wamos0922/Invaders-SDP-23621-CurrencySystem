package skilltree;

/**
 * Numbers of the Salvage branch (GOG-89), read by the game when a run or
 * level starts.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillSalvage {

	/** Scavenger: extra coin drop chance (+2 percentage points). */
	public static final double SCAVENGER_COIN_CHANCE = 0.02;
	/** Lucky Find: extra item drop chance (+3 percentage points). */
	public static final double LUCKY_FIND_ITEM_CHANCE = 0.03;
	/** Long Fuse: extra time dropped items stay on the ground. */
	public static final long LONG_FUSE_MILLIS = 3000;
	/** Shard Hunter: factor for shard rewards (used by GOG-32). */
	public static final double SHARD_HUNTER_FACTOR = 1.10;

	private SkillSalvage() {
	}

	/**
	 * @param effects
	 *            Unlocked nodes.
	 * @return Extra coin drop chance (0 or 0.02).
	 */
	public static double coinChanceBonus(final SkillEffects effects) {
		return effects.has(SkillTreeCatalog.SCAVENGER) ? SCAVENGER_COIN_CHANCE
				: 0;
	}

	/**
	 * @param effects
	 *            Unlocked nodes.
	 * @return Extra item drop chance (0 or 0.03).
	 */
	public static double itemChanceBonus(final SkillEffects effects) {
		return effects.has(SkillTreeCatalog.LUCKY_FIND)
				? LUCKY_FIND_ITEM_CHANCE : 0;
	}

	/**
	 * @param effects
	 *            Unlocked nodes.
	 * @return Extra ground time for drops (0 or 3000 ms).
	 */
	public static long groundTimeBonus(final SkillEffects effects) {
		return effects.has(SkillTreeCatalog.LONG_FUSE) ? LONG_FUSE_MILLIS : 0;
	}

	/**
	 * For GOG-32: multiply shard rewards by this.
	 *
	 * @param effects
	 *            Unlocked nodes.
	 * @return 1.10 with Shard Hunter, otherwise 1.0.
	 */
	public static double shardRewardFactor(final SkillEffects effects) {
		return effects.has(SkillTreeCatalog.SHARD_HUNTER) ? SHARD_HUNTER_FACTOR
				: 1.0;
	}

	/**
	 * Fast Attack's Cooled Barrel.
	 *
	 * @param effects
	 *            Unlocked nodes.
	 * @return Extra Rapid Fire stacks (0 or 1).
	 */
	public static int rapidFireExtraStacks(final SkillEffects effects) {
		return effects.has(SkillTreeCatalog.COOLED_BARREL) ? 1 : 0;
	}
}
