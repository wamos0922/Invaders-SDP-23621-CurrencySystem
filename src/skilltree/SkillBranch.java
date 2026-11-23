package skilltree;

/**
 * The three branches of a ship's skill tree, left to right on screen.
 *
 * @author GoG - Progression (GOG-70)
 */
public enum SkillBranch {
	/** Survival nodes, the same on every ship. */
	ARMOR("Armor", true, 4),
	/** The ship's own nodes, different for each ship. */
	SPECIALTY("Specialty", false, 7),
	/** Drop and economy nodes, the same on every ship. */
	SALVAGE("Salvage", true, 4);

	/** Name shown above the column. */
	private final String displayName;
	/** True if every ship shares this branch. */
	private final boolean shared;
	/** Number of nodes in the branch. */
	private final int size;

	SkillBranch(final String displayName, final boolean shared,
			final int size) {
		this.displayName = displayName;
		this.shared = shared;
		this.size = size;
	}

	/** @return Name shown above the column. */
	public String getDisplayName() {
		return this.displayName;
	}

	/** @return True if every ship shares this branch. */
	public boolean isShared() {
		return this.shared;
	}

	/** @return Number of nodes in the branch. */
	public int size() {
		return this.size;
	}
}
