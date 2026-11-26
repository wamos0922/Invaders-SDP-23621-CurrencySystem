package skilltree;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The player's shards (GOG-10): how many of each rarity they have. Saved
 * to the "shards" file, one line per rarity, e.g. "rare=12". Shards are
 * earned in the GOG-32 game modes, paid for skill tree nodes (GOG-88) and
 * traded in the converter (GOG-90).
 *
 * To test before GOG-32 exists, close the game and edit the "shards" file
 * next to the other saves, e.g. "common=200".
 *
 * @author GoG - Progression (GOG-70)
 */
public final class ShardWallet {

	/** Save file name. */
	private static final String FILE_NAME = "shards";
	/** Shared instance. */
	private static ShardWallet instance;

	/** Shards per rarity. */
	private final Map<ShardType, Integer> counts =
			new EnumMap<ShardType, Integer>(ShardType.class);
	/** Where the shards are saved. */
	private final File file;
	/** Game logger. */
	private final Logger logger = Logger.getLogger(ShardWallet.class
			.getName());

	private ShardWallet(final File file) {
		this.file = file;
		Properties saved = SaveFile.read(file);
		for (ShardType type : ShardType.values())
			this.counts.put(type, SaveFile.count(saved, key(type)));
	}

	/** @return The player's wallet, saved next to the game. */
	public static synchronized ShardWallet getInstance() {
		if (instance == null)
			instance = new ShardWallet(SaveFile.locate(FILE_NAME));
		return instance;
	}

	/**
	 * A wallet saved to a given file, for tests.
	 *
	 * @param file
	 *            Save file.
	 * @return A new wallet reading and writing that file.
	 */
	public static ShardWallet forFile(final File file) {
		return new ShardWallet(file);
	}

	/**
	 * @param type
	 *            Shard rarity.
	 * @return How many the player has.
	 */
	public int get(final ShardType type) {
		return this.counts.get(type);
	}

	/**
	 * @param cost
	 *            Amount per rarity, in {@link ShardType} order.
	 * @return True if the player has at least that many of every rarity.
	 */
	public boolean canAfford(final int[] cost) {
		for (ShardType type : ShardType.values())
			if (get(type) < cost[type.ordinal()])
				return false;
		return true;
	}

	/**
	 * Takes shards away, all or nothing.
	 *
	 * @param cost
	 *            Amount per rarity, in {@link ShardType} order.
	 * @return False (and nothing changes) if the player can't afford it or
	 *         the wallet could not be saved.
	 */
	public boolean spend(final int[] cost) {
		if (!canAfford(cost))
			return false;
		int[] change = new int[cost.length];
		for (int i = 0; i < cost.length; i++)
			change[i] = -cost[i];
		return apply(change);
	}

	/**
	 * Gives shards, e.g. a GOG-32 reward.
	 *
	 * @param type
	 *            Shard rarity.
	 * @param amount
	 *            How many, 0 or more.
	 * @return False (and nothing changes) if the wallet could not be saved.
	 */
	public boolean add(final ShardType type, final int amount) {
		if (amount < 0)
			throw new IllegalArgumentException("amount must be 0 or more");
		int[] change = new int[ShardType.values().length];
		change[type.ordinal()] = amount;
		return apply(change);
	}

	/**
	 * Adds and removes shards in one save, all or nothing. Used by the
	 * converter, which takes one rarity and gives another.
	 *
	 * @param change
	 *            Amount to add per rarity (negative to take), in
	 *            {@link ShardType} order.
	 * @return False (and nothing changes) if a rarity would go below 0 or
	 *         the wallet could not be saved.
	 */
	public boolean apply(final int[] change) {
		for (ShardType type : ShardType.values()) {
			// long, so a big change can't wrap around past the int limit.
			long after = (long) get(type) + change[type.ordinal()];
			if (after < 0 || after > Integer.MAX_VALUE)
				return false;
		}
		Map<ShardType, Integer> before =
				new EnumMap<ShardType, Integer>(this.counts);
		for (ShardType type : ShardType.values())
			this.counts.put(type, get(type) + change[type.ordinal()]);
		if (!save()) {
			this.counts.putAll(before);
			this.logger.warning("Couldn't save shards, change undone.");
			return false;
		}
		return true;
	}

	private boolean save() {
		Properties values = new Properties();
		for (ShardType type : ShardType.values())
			values.setProperty(key(type), Integer.toString(get(type)));
		return SaveFile.write(this.file, values,
				"Shards the player owns (GOG-10)");
	}

	private static String key(final ShardType type) {
		return type.name().toLowerCase();
	}
}
