package skilltree;

/**
 * Shard conversion rules (GOG-90). Upgrading costs 5 shards for 1 of the
 * next rarity; breaking down gives 3 of the rarity below for 1. Over
 * several rarity steps the rates multiply (25 Common for 1 Epic, 1 Mythic
 * for 81 Common), so a round trip always loses shards.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class ShardConverter {

	/** Shards given for 1 of the next rarity. */
	public static final int UP_RATE = 5;
	/** Shards received for 1 of the rarity above. */
	public static final int DOWN_RATE = 3;

	private ShardConverter() {
	}

	/**
	 * @param give
	 *            Rarity given.
	 * @param get
	 *            Rarity received.
	 * @return Shards given for one conversion.
	 */
	public static int givePerConversion(final ShardType give,
			final ShardType get) {
		int steps = steps(give, get);
		return steps > 0 ? power(UP_RATE, steps) : 1;
	}

	/**
	 * @param give
	 *            Rarity given.
	 * @param get
	 *            Rarity received.
	 * @return Shards received for one conversion.
	 */
	public static int getPerConversion(final ShardType give,
			final ShardType get) {
		int steps = steps(give, get);
		return steps > 0 ? 1 : power(DOWN_RATE, -steps);
	}

	/**
	 * @param wallet
	 *            Player's wallet.
	 * @param give
	 *            Rarity given.
	 * @param get
	 *            Rarity received.
	 * @return Most conversions the wallet can pay for (can be 0).
	 */
	public static int maxConversions(final ShardWallet wallet,
			final ShardType give, final ShardType get) {
		if (give == get)
			return 0;
		return wallet.get(give) / givePerConversion(give, get);
	}

	/**
	 * Converts shards in the wallet, all or nothing.
	 *
	 * @param wallet
	 *            Player's wallet.
	 * @param give
	 *            Rarity given.
	 * @param get
	 *            Rarity received; must differ from give.
	 * @param conversions
	 *            How many conversions, 1 or more.
	 * @return False (and nothing changes) if the rarities are the same,
	 *         the wallet can't pay, or saving failed.
	 */
	public static boolean convert(final ShardWallet wallet,
			final ShardType give, final ShardType get,
			final int conversions) {
		if (give == get || conversions < 1
				|| conversions > maxConversions(wallet, give, get))
			return false;
		int[] change = new int[ShardType.values().length];
		change[give.ordinal()] = -givePerConversion(give, get) * conversions;
		change[get.ordinal()] = getPerConversion(give, get) * conversions;
		return wallet.apply(change);
	}

	/** @return Rarity steps from give to get; positive = upgrade. */
	private static int steps(final ShardType give, final ShardType get) {
		return get.ordinal() - give.ordinal();
	}

	private static int power(final int base, final int exponent) {
		int result = 1;
		for (int i = 0; i < exponent; i++)
			result *= base;
		return result;
	}
}
