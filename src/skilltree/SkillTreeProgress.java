package skilltree;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import entity.Ship.ShipType;

/**
 * Which skill nodes the player has unlocked (GOG-88). Nodes unlock in
 * order, so it is enough to keep how many nodes of each branch are owned:
 * Armor and Salvage once for every ship, Specialty per ship. Saved to the
 * "skill-tree" file, e.g. "armor=2", "specialty.FAST_MOVE=3".
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillTreeProgress {

	/** Result of trying to unlock a node. */
	public enum Unlock {
		/** Unlocked and paid. */
		OK,
		/** Already owned. */
		ALREADY_OWNED,
		/** The node above is not owned yet. */
		LOCKED,
		/** Not enough shards. */
		NOT_ENOUGH_SHARDS,
		/** Could not be saved; nothing changed. */
		SAVE_FAILED
	}

	/** Save file name. */
	private static final String FILE_NAME = "skill-tree";
	/** Shared instance. */
	private static SkillTreeProgress instance;

	/** Owned Armor nodes (shared). */
	private int armor;
	/** Owned Salvage nodes (shared). */
	private int salvage;
	/** Owned Specialty nodes per ship. */
	private final Map<ShipType, Integer> specialty =
			new EnumMap<ShipType, Integer>(ShipType.class);
	/** Where progress is saved. */
	private final File file;
	/** Wallet that pays for nodes. */
	private final ShardWallet wallet;

	private SkillTreeProgress(final File file, final ShardWallet wallet) {
		this.file = file;
		this.wallet = wallet;
		Properties saved = SaveFile.read(file);
		this.armor = Math.min(SkillBranch.ARMOR.size(),
				SaveFile.count(saved, "armor"));
		this.salvage = Math.min(SkillBranch.SALVAGE.size(),
				SaveFile.count(saved, "salvage"));
		for (ShipType ship : ShipType.values())
			this.specialty.put(ship, Math.min(SkillBranch.SPECIALTY.size(),
					SaveFile.count(saved, "specialty." + ship.name())));
	}

	/** @return The player's progress, saved next to the game. */
	public static synchronized SkillTreeProgress getInstance() {
		if (instance == null)
			instance = new SkillTreeProgress(SaveFile.locate(FILE_NAME),
					ShardWallet.getInstance());
		return instance;
	}

	/**
	 * Progress saved to a given file and paid from a given wallet, for
	 * tests.
	 *
	 * @param file
	 *            Save file.
	 * @param wallet
	 *            Wallet to pay from.
	 * @return New progress reading and writing that file.
	 */
	public static SkillTreeProgress forFile(final File file,
			final ShardWallet wallet) {
		return new SkillTreeProgress(file, wallet);
	}

	/**
	 * @param ship
	 *            Ship (only matters for Specialty).
	 * @param branch
	 *            Branch.
	 * @return How many nodes of the branch are owned.
	 */
	public int owned(final ShipType ship, final SkillBranch branch) {
		switch (branch) {
			case ARMOR:
				return this.armor;
			case SALVAGE:
				return this.salvage;
			default:
				return this.specialty.get(ship);
		}
	}

	/**
	 * @param ship
	 *            Ship whose page is shown.
	 * @param node
	 *            Node.
	 * @return OWNED, AVAILABLE (next to buy) or LOCKED.
	 */
	public NodeState state(final ShipType ship, final SkillNode node) {
		int owned = owned(ship, node.getBranch());
		return node.getIndex() < owned ? NodeState.OWNED
				: node.getIndex() == owned ? NodeState.AVAILABLE
				: NodeState.LOCKED;
	}

	/**
	 * @param ship
	 *            Ship.
	 * @param nodeId
	 *            Node id, e.g. {@link SkillTreeCatalog#QUICK_REPAIR}.
	 * @return True if the ship has that node (shared nodes count for every
	 *         ship).
	 */
	public boolean has(final ShipType ship, final String nodeId) {
		for (SkillBranch branch : SkillBranch.values()) {
			List<SkillNode> nodes = SkillTreeCatalog.nodes(ship, branch);
			for (int i = 0; i < owned(ship, branch); i++)
				if (nodes.get(i).getId().equals(nodeId))
					return true;
		}
		return false;
	}

	/**
	 * @param ship
	 *            Ship.
	 * @return Owned nodes of all three branches on that ship's page.
	 */
	public int ownedCount(final ShipType ship) {
		int count = 0;
		for (SkillBranch branch : SkillBranch.values())
			count += owned(ship, branch);
		return count;
	}

	/**
	 * Buys a node: it must be the next one in its branch, and the wallet
	 * must have every shard it costs. Pays and saves, all or nothing.
	 *
	 * @param ship
	 *            Ship whose page is shown.
	 * @param node
	 *            Node to unlock.
	 * @return What happened.
	 */
	public Unlock unlock(final ShipType ship, final SkillNode node) {
		NodeState state = state(ship, node);
		if (state == NodeState.OWNED)
			return Unlock.ALREADY_OWNED;
		if (state == NodeState.LOCKED)
			return Unlock.LOCKED;
		int[] cost = node.getCostArray();
		if (!this.wallet.canAfford(cost))
			return Unlock.NOT_ENOUGH_SHARDS;
		if (!this.wallet.spend(cost))
			return Unlock.SAVE_FAILED;
		setOwned(ship, node.getBranch(), node.getIndex() + 1);
		if (!save()) {
			// Undo: give the shards back and forget the node.
			setOwned(ship, node.getBranch(), node.getIndex());
			this.wallet.apply(cost);
			return Unlock.SAVE_FAILED;
		}
		return Unlock.OK;
	}

	private void setOwned(final ShipType ship, final SkillBranch branch,
			final int count) {
		switch (branch) {
			case ARMOR:
				this.armor = count;
				break;
			case SALVAGE:
				this.salvage = count;
				break;
			default:
				this.specialty.put(ship, count);
				break;
		}
	}

	private boolean save() {
		Properties values = new Properties();
		values.setProperty("armor", Integer.toString(this.armor));
		values.setProperty("salvage", Integer.toString(this.salvage));
		for (ShipType ship : ShipType.values())
			values.setProperty("specialty." + ship.name(),
					Integer.toString(this.specialty.get(ship)));
		return SaveFile.write(this.file, values,
				"Unlocked skill tree nodes (GOG-88)");
	}
}
