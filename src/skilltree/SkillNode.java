package skilltree;

/**
 * One node of a skill tree: what it is called, what it does and what it
 * costs. Whether the player owns it is not stored here (see
 * {@link NodeState}); GOG-88 adds that.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillNode {
	/** Branch the node belongs to. */
	private final SkillBranch branch;
	/** Position in its branch, 0 = first (top). */
	private final int index;
	/** Name shown on the node. */
	private final String name;
	/** What the node does, shown in the detail panel. */
	private final String description;
	/** Cost per shard type, in {@link ShardType} order. */
	private final int[] cost;

	/**
	 * @param branch
	 *            Branch the node belongs to.
	 * @param index
	 *            Position in its branch, 0 = first.
	 * @param name
	 *            Name shown on the node.
	 * @param description
	 *            What the node does.
	 * @param cost
	 *            Cost per shard type, in {@link ShardType} order
	 *            (common, rare, epic, legendary, mythic).
	 */
	public SkillNode(final SkillBranch branch, final int index,
			final String name, final String description, final int[] cost) {
		if (cost.length != ShardType.values().length)
			throw new IllegalArgumentException("cost needs one value per"
					+ " shard type");
		this.branch = branch;
		this.index = index;
		this.name = name;
		this.description = description;
		this.cost = cost.clone();
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
