package tech.mistermel.edisoncontrol.navigation;

public class Waypoint {

	private float x, y;
	
	protected Waypoint(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
}
