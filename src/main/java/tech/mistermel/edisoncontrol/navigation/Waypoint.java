package tech.mistermel.edisoncontrol.navigation;

public class Waypoint {
	
	// This class is currently basically just a Location, however in the future extra
	// field may be added to a waypoint so I'm keeping it this way.

	private Location loc;
	
	protected Waypoint(float x, float y) {
		this.loc = new Location(x, y);
	}
	
	public Location getLocation() {
		return loc;
	}
	
}
