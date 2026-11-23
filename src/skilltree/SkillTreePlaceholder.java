package skilltree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import entity.Ship.ShipType;

/**
 * TEMPORARY data for the skill tree page (GOG-51), so the screen can be
 * built before the real logic exists. GOG-88 replaces this class with the
 * real node list, the player's progress and the shard wallet; the screen
 * only calls the three methods below, so it will not need to change.
 *
 * Costs are already the real ones from
 * docs/design/skill-tree-and-items.md.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillTreePlaceholder {

	/** Cost of shared nodes 1-4: common, rare, epic, legendary, mythic. */
	private static final int[][] SHARED_COST = {
			{ 10, 0, 0, 0, 0 },
			{ 20, 5, 0, 0, 0 },
			{ 30, 10, 3, 0, 0 },
			{ 45, 15, 6, 2, 0 } };
	/** Cost of Specialty nodes 1-7. */
	private static final int[][] SPECIALTY_COST = {
			{ 15, 3, 0, 0, 0 },
			{ 25, 6, 1, 0, 0 },
			{ 35, 10, 3, 0, 0 },
			{ 45, 14, 5, 1, 0 },
			{ 60, 18, 8, 2, 0 },
			{ 75, 24, 10, 4, 1 },
			{ 100, 30, 14, 6, 3 } };

	private SkillTreePlaceholder() {
	}

	/**
	 * @param ship
	 *            Ship whose page is shown.
	 * @param branch
	 *            Branch (column).
	 * @return The branch's nodes, top to bottom.
	 */
	public static List<SkillNode> nodes(final ShipType ship,
			final SkillBranch branch) {
		List<SkillNode> nodes = new ArrayList<SkillNode>();
		int[][] costs = branch == SkillBranch.SPECIALTY ? SPECIALTY_COST
				: SHARED_COST;
		for (int i = 0; i < branch.size(); i++) {
			boolean capstone = branch == SkillBranch.SPECIALTY
					&& i == branch.size() - 1;
			String name = capstone ? "Capstone"
					: branch.getDisplayName() + " " + (i + 1);
			nodes.add(new SkillNode(branch, i, name,
					"Placeholder - real effect comes in GOG-88.", costs[i]));
		}
		return Collections.unmodifiableList(nodes);
	}

	/**
	 * @param ship
	 *            Ship whose page is shown.
	 * @param node
	 *            Node.
	 * @return Placeholder state: the first node of each branch is
	 *         available, the rest are locked.
	 */
	public static NodeState state(final ShipType ship, final SkillNode node) {
		return node.getIndex() == 0 ? NodeState.AVAILABLE : NodeState.LOCKED;
	}

	/**
	 * @param type
	 *            Shard type.
	 * @return Placeholder balance (0 until shards exist).
	 */
	public static int shards(final ShardType type) {
		return 0;
	}
}
