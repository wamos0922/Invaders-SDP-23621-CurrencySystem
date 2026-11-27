package skilltree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import entity.Ship.ShipType;

/**
 * Every skill tree node (GOG-88): the shared Armor and Salvage branches and
 * each ship's Specialty branch, with the names, effects and costs from
 * docs/design/skill-tree-and-items.md. Nodes with a special effect have an
 * id constant here, so the game code (GOG-89, GOG-91) can check for them.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillTreeCatalog {

	// --- Ids of nodes with a special effect (used by GOG-89 / GOG-91) ---

	/** Armor 1: longer invincibility after losing a life. */
	public static final String QUICK_REPAIR = "quick_repair";
	/** Armor 2: short shield when a level starts. */
	public static final String LAUNCH_SHIELD = "launch_shield";
	/** Armor 3: shield when dropping to 1 life. */
	public static final String LAST_STAND = "last_stand";
	/** Armor 4: first hit of a level is blocked. */
	public static final String HULL_PLATING = "hull_plating";
	/** Salvage 1: more coin drops. */
	public static final String SCAVENGER = "scavenger";
	/** Salvage 2: more item drops. */
	public static final String LUCKY_FIND = "lucky_find";
	/** Salvage 3: drops stay longer. */
	public static final String LONG_FUSE = "long_fuse";
	/** Salvage 4: more shards from rewards (used by GOG-32). */
	public static final String SHARD_HUNTER = "shard_hunter";
	/** Standard 5: item pickups speed up shooting. */
	public static final String PICKUP_RUSH = "standard.pickup_rush";
	/** Standard capstone. */
	public static final String ADAPTIVE_CORE = "standard.adaptive_core";
	/** Fast Move 3: faster shots while moving. */
	public static final String SLIPSTREAM = "fast_move.slipstream";
	/** Fast Move 6: score for close dodges. */
	public static final String NEAR_MISS = "fast_move.near_miss";
	/** Fast Move capstone. */
	public static final String BLINK_DASH = "fast_move.blink_dash";
	/** Fast Attack 2: Rapid Fire stacks one higher. */
	public static final String COOLED_BARREL = "fast_attack.cooled_barrel";
	/** Fast Attack 5: every 10th shot pierces. */
	public static final String HOT_ROUNDS = "fast_attack.hot_rounds";
	/** Fast Attack capstone. */
	public static final String BULLET_STORM = "fast_attack.bullet_storm";
	/** Two-Way 2: the two bullets spread wider. */
	public static final String WIDE_BARRELS = "two_way.wide_barrels";
	/** Two-Way 5: bonus score when both bullets kill. */
	public static final String CROSSFIRE = "two_way.crossfire";
	/** Two-Way capstone. */
	public static final String TRIPLE_BARREL = "two_way.triple_barrel";
	/** Big Bullet 3: bullets pierce one enemy. */
	public static final String PIERCING_SHELL = "big_bullet.piercing_shell";
	/** Big Bullet 5: every 5th boss hit counts twice. */
	public static final String HEAVY_IMPACT = "big_bullet.heavy_impact";
	/** Big Bullet capstone. */
	public static final String SHOCKWAVE = "big_bullet.shockwave";

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

	/** Armor nodes, shared by every ship. */
	private static final List<SkillNode> ARMOR;
	/** Salvage nodes, shared by every ship. */
	private static final List<SkillNode> SALVAGE;
	/** Specialty nodes of each ship. */
	private static final Map<ShipType, List<SkillNode>> SPECIALTY =
			new EnumMap<ShipType, List<SkillNode>>(ShipType.class);

	static {
		ARMOR = branch(SkillBranch.ARMOR,
				node(QUICK_REPAIR, "Quick Repair",
						"+0.5 s invincible after losing a life"),
				node(LAUNCH_SHIELD, "Launch Shield",
						"Start every level with a 2 s shield"),
				node(LAST_STAND, "Last Stand",
						"Drop to 1 life: 3 s shield, once per level"),
				node(HULL_PLATING, "Hull Plating",
						"The first hit in each level is blocked"));
		SALVAGE = branch(SkillBranch.SALVAGE,
				node(SCAVENGER, "Scavenger",
						"Coin drop chance +2% (10% to 12%)"),
				node(LUCKY_FIND, "Lucky Find",
						"Item drop chance +3% (15% to 18%)"),
				node(LONG_FUSE, "Long Fuse",
						"Dropped items stay 3 s longer"),
				node(SHARD_HUNTER, "Shard Hunter",
						"+10% shards from rewards"));

		SPECIALTY.put(ShipType.STANDARD, branch(SkillBranch.SPECIALTY,
				stat("standard.tuned_engine", "Tuned Engine",
						"+10% move speed", SkillStat.SPEED, 10),
				stat("standard.quick_loader", "Quick Loader",
						"+8% fire rate (750 to 694 ms)", SkillStat.FIRE_RATE, 8),
				stat("standard.fast_rounds", "Fast Rounds",
						"+15% bullet speed", SkillStat.BULLET_SPEED, 15),
				stat("standard.quick_loader_2", "Quick Loader II",
						"+8% fire rate (to 643 ms)", SkillStat.FIRE_RATE, 8),
				node(PICKUP_RUSH, "Pickup Rush",
						"Item pickup: +20% fire rate for 3 s"),
				stat("standard.tuned_engine_2", "Tuned Engine II",
						"+10% move speed (+20% total)", SkillStat.SPEED, 10),
				node(ADAPTIVE_CORE, "Adaptive Core",
						"Every 15 s, your next 5 shots pierce")));
		SPECIALTY.put(ShipType.FAST_MOVE, branch(SkillBranch.SPECIALTY,
				stat("fast_move.afterburner", "Afterburner",
						"+8% move speed", SkillStat.SPEED, 8),
				stat("fast_move.quick_loader", "Quick Loader",
						"+8% fire rate (750 to 694 ms)", SkillStat.FIRE_RATE, 8),
				node(SLIPSTREAM, "Slipstream",
						"Shots fired while moving are 20% faster"),
				stat("fast_move.afterburner_2", "Afterburner II",
						"+8% move speed (+16% total)", SkillStat.SPEED, 8),
				stat("fast_move.quick_loader_2", "Quick Loader II",
						"+8% fire rate (to 643 ms)", SkillStat.FIRE_RATE, 8),
				node(NEAR_MISS, "Near Miss",
						"Close dodge of an enemy bullet: +10 score"),
				node(BLINK_DASH, "Blink Dash",
						"Double-tap left/right: dash, 4 s cooldown")));
		SPECIALTY.put(ShipType.FAST_ATTACK, branch(SkillBranch.SPECIALTY,
				stat("fast_attack.hair_trigger", "Hair Trigger",
						"+6% fire rate (375 to 354 ms)", SkillStat.FIRE_RATE, 6),
				node(COOLED_BARREL, "Cooled Barrel",
						"Rapid Fire pickups stack 1 higher"),
				stat("fast_attack.steady_hands", "Steady Hands",
						"+10% move speed", SkillStat.SPEED, 10),
				stat("fast_attack.hair_trigger_2", "Hair Trigger II",
						"+6% fire rate (to 334 ms)", SkillStat.FIRE_RATE, 6),
				node(HOT_ROUNDS, "Hot Rounds",
						"Every 10th shot pierces one enemy"),
				stat("fast_attack.hair_trigger_3", "Hair Trigger III",
						"+6% fire rate (to 315 ms)", SkillStat.FIRE_RATE, 6),
				node(BULLET_STORM, "Bullet Storm",
						"Every 20 s, fire twice as fast for 3 s")));
		SPECIALTY.put(ShipType.TWO_WAY, branch(SkillBranch.SPECIALTY,
				stat("two_way.stabilizers", "Stabilizers",
						"+25% move speed", SkillStat.SPEED, 25),
				node(WIDE_BARRELS, "Wide Barrels",
						"The two bullets spread slightly wider"),
				stat("two_way.quick_loader", "Quick Loader",
						"+10% fire rate (1000 to 909 ms)", SkillStat.FIRE_RATE, 10),
				stat("two_way.stabilizers_2", "Stabilizers II",
						"+25% move speed (+50% total)", SkillStat.SPEED, 25),
				node(CROSSFIRE, "Crossfire",
						"Both bullets kill: +50% score for them"),
				stat("two_way.quick_loader_2", "Quick Loader II",
						"+10% fire rate (to 826 ms)", SkillStat.FIRE_RATE, 10),
				node(TRIPLE_BARREL, "Triple Barrel",
						"Every 3rd volley adds a middle bullet")));
		SPECIALTY.put(ShipType.BIG_BULLET, branch(SkillBranch.SPECIALTY,
				stat("big_bullet.dense_core", "Dense Core",
						"Bullet 10% bigger", SkillStat.BULLET_SIZE, 10),
				stat("big_bullet.faster_reload", "Faster Reload",
						"+8% fire rate (1000 to 926 ms)", SkillStat.FIRE_RATE, 8),
				node(PIERCING_SHELL, "Piercing Shell",
						"Bullets pierce one enemy"),
				stat("big_bullet.faster_reload_2", "Faster Reload II",
						"+8% fire rate (to 857 ms)", SkillStat.FIRE_RATE, 8),
				node(HEAVY_IMPACT, "Heavy Impact",
						"Every 5th boss hit counts twice"),
				stat("big_bullet.dense_core_2", "Dense Core II",
						"Bullet 10% bigger (+20% total)", SkillStat.BULLET_SIZE, 10),
				node(SHOCKWAVE, "Shockwave",
						"Hits also destroy enemies next to the target")));
	}

	private SkillTreeCatalog() {
	}

	/**
	 * @param ship
	 *            Ship whose page is shown (ignored for shared branches).
	 * @param branch
	 *            Branch (column).
	 * @return The branch's nodes, top to bottom.
	 */
	public static List<SkillNode> nodes(final ShipType ship,
			final SkillBranch branch) {
		switch (branch) {
			case ARMOR:
				return ARMOR;
			case SALVAGE:
				return SALVAGE;
			default:
				return SPECIALTY.get(ship);
		}
	}

	// --- Building the lists ---

	/** A node before it knows its branch and position. */
	private static final class Draft {
		private final String id;
		private final String name;
		private final String description;
		private final SkillStat stat;
		private final int percent;

		Draft(final String id, final String name, final String description,
				final SkillStat stat, final int percent) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.stat = stat;
			this.percent = percent;
		}
	}

	private static Draft node(final String id, final String name,
			final String description) {
		return new Draft(id, name, description, null, 0);
	}

	private static Draft stat(final String id, final String name,
			final String description, final SkillStat stat,
			final int percent) {
		return new Draft(id, name, description, stat, percent);
	}

	private static List<SkillNode> branch(final SkillBranch branch,
			final Draft... drafts) {
		if (drafts.length != branch.size())
			throw new IllegalStateException(branch + " needs " + branch.size()
					+ " nodes");
		int[][] costs = branch == SkillBranch.SPECIALTY ? SPECIALTY_COST
				: SHARED_COST;
		List<SkillNode> nodes = new ArrayList<SkillNode>();
		for (int i = 0; i < drafts.length; i++)
			nodes.add(new SkillNode(branch, i, drafts[i].id, drafts[i].name,
					drafts[i].description, costs[i], drafts[i].stat,
					drafts[i].percent));
		return Collections.unmodifiableList(nodes);
	}
}
