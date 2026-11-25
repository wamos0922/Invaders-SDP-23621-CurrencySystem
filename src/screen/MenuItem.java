package screen;

/** Main-menu entries. */
public enum MenuItem {
	MAIN_STORY("Story",2,true),
	ENDLESS("Endless game",8,true),
	CHALLENGES("Challenges",ChallengeMenuScreen.CODE,true), // GOG-102
	SHIP_SELECT("Customize",7,true),
	SHOP("Shop",5,true),
	ACHIEVEMENTS("Achievements",6,true),
	HIGH_SCORES("High scores",3,true),
	SETTINGS("Settings",4,true),
	EXIT("Exit",0,true);

	private final String title;
	private final int code;
	private final boolean enabled;
	MenuItem(String title,int code,boolean enabled) {
		this.title=title;this.code=code;this.enabled=enabled;
	}
	public String getTitle() { return title; }
	public int getCode() { return code; }
	public boolean isEnabled() { return enabled; }
	public static MenuItem first() { return values()[0]; }
	public MenuItem next() { return step(1); }
	public MenuItem previous() { return step(-1); }
	private MenuItem step(int direction) {
		MenuItem[] items=values();
		return items[Math.floorMod(ordinal()+direction,items.length)];
	}
}
