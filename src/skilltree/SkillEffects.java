package skilltree;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import entity.Ship.ShipType;

/**
 * What a ship's unlocked skill nodes do in a run (GOG-89): the total stat
 * bonuses and which special nodes it has. Made once when a level starts,
 * so the game never reads save files mid-level.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillEffects {

	/** Total percent bonus per stat. */
	private final Map<SkillStat, Integer> percents =
			new EnumMap<SkillStat, Integer>(SkillStat.class);
	/** Ids of every owned node. */
	private final Set<String> owned = new HashSet<String>();

	private SkillEffects() {
		for (SkillStat stat : SkillStat.values())
			this.percents.put(stat, 0);
	}

	/**
	 * @param ship
	 *            Ship flown in the run.
	 * @return The effects of that ship's unlocked nodes.
	 */
	public static SkillEffects forShip(final ShipType ship) {
		return of(ship, SkillTreeProgress.getInstance());
	}

	/**
	 * @param ship
	 *            Ship flown in the run.
	 * @param progress
	 *            Unlocked nodes (a test can pass its own).
	 * @return The effects of that ship's unlocked nodes.
	 */
	public static SkillEffects of(final ShipType ship,
			final SkillTreeProgress progress) {
		SkillEffects effects = new SkillEffects();
		for (SkillBranch branch : SkillBranch.values()) {
			List<SkillNode> nodes = SkillTreeCatalog.nodes(ship, branch);
			for (int i = 0; i < progress.owned(ship, branch); i++) {
				SkillNode node = nodes.get(i);
				effects.owned.add(node.getId());
				if (node.getStat() != null)
					effects.percents.put(node.getStat(), effects.percents
							.get(node.getStat()) + node.getStatPercent());
			}
		}
		return effects;
	}

	/** @return No effects at all (no nodes unlocked). */
	public static SkillEffects none() {
		return new SkillEffects();
	}

	/**
	 * @param stat
	 *            Stat.
	 * @return Total bonus in percent, e.g. 16 for +16%.
	 */
	public int percent(final SkillStat stat) {
		return this.percents.get(stat);
	}

	/**
	 * @param stat
	 *            Stat.
	 * @return Factor to multiply the base value by, e.g. 1.16.
	 */
	public double multiplier(final SkillStat stat) {
		return 1.0 + percent(stat) / 100.0;
	}

	/**
	 * @param nodeId
	 *            Node id, e.g. {@link SkillTreeCatalog#QUICK_REPAIR}.
	 * @return True if that node is unlocked for this ship.
	 */
	public boolean has(final String nodeId) {
		return this.owned.contains(nodeId);
	}
}
