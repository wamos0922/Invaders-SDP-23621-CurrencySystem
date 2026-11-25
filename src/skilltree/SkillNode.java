package skilltree;


/**
 * One node of a skill tree: its id, what it is called, what it does, what
 * it costs and, for stat nodes, which stat it raises by how much. Whether
 * the player owns it is kept by {@link SkillTreeProgress} (GOG-88).
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillNode {
	/** Branch the node belongs to. */
	private final SkillBranch branch;
	/** Position in its branch, 0 = first (top). */
	private final int index;
	/** Unique id, e.g. "quick_repair" or "fast_move.blink_dash". */
	private final String id;
	/** Name shown on the node. */
	private final String name;
	/** What the node does, shown in the detail panel. */
	private final String description;
	/** Cost per shard type, in {@link ShardType} order. */
	private final int[] cost;
	/** Stat this node raises, or null for a special effect. */
	private final SkillStat stat;
	/** How much the stat goes up, in percent. */
	private final int statPercent;

	/**
	 * @param branch
	 *            Branch the node belongs to.
	 * @param index
	 *            Position in its branch, 0 = first.
	 * @param id
	 *            Unique id of the node.
	 * @param name
	 *            Name shown on the node.
	 * @param description
	 *            What the node does.
	 * @param cost
	 *            Cost per shard type, in {@link ShardType} order
	 *            (common, rare, epic, legendary, mythic).
	 * @param stat
	 *            Stat the node raises, or null for a special effect.
	 * @param statPercent
	 *            How much the stat goes up, in percent (0 if no stat).
	 */
	public SkillNode(final SkillBranch branch, final int index,
			final String id, final String name, final String description,
			final int[] cost, final SkillStat stat, final int statPercent) {
		if (cost.length != ShardType.values().length)
			throw new IllegalArgumentException("cost needs one value per"
					+ " shard type");
				this.branch = branch;
		this.index = index;
		this.id = id;
		this.name = name;
		this.description = description;
		this.cost = cost.clone();
		this.stat = stat;
		this.statPercent = stat == null ? 0 : statPercent;
	}

	/** @return Unique id of the node. */
	public String getId() {
		return this.id;
	}

	/** @return Stat the node raises, or null for a special effect. */
	public SkillStat getStat() {
		return this.stat;
	}

	/** @return How much the stat goes up, in percent. */
	public int getStatPercent() {
		return this.statPercent;
	}

	/** @return Cost per shard type, in {@link ShardType} order (a copy). */
	public int[] getCostArray() {
		return this.cost.clone();
	}

	/** @return Branch the node belongs to. */
	public SkillBranch getBranch() {
		return this.branch;
	}

	/** @return Position in its branch, 0 = first. */
	public int getIndex() {
		return this.index;
	}

	/** @return True for the last node of the Specialty branch. */
	public boolean isCapstone() {
		return this.branch == SkillBranch.SPECIALTY
				&& this.index == SkillBranch.SPECIALTY.size() - 1;
	}

	/** @return Name shown on the node. */
	public String getName() {
		return this.name;
	}

	/** @return What the node does. */
	public String getDescription() {
		return this.description;
	}

	/**
	 * @param type
	 *            Shard type.
	 * @return How many of that shard the node costs.
	 */
	public int getCost(final ShardType type) {
		return this.cost[type.ordinal()];
	}
}
