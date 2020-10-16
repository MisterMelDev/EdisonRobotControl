package tech.mistermel.edisoncontrol.navigation;

public class Location {

	private float x;
	private float y;
	
	public Location() {}
	
	public Location(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public double distanceTo(Location loc2) {
		return Math.sqrt(Math.pow(x - loc2.getX(), 2) + Math.pow(y - loc2.getY(), 2));
	}
	
	public double headingTo(Location loc2) {
		double radians = Math.atan2(loc2.getY() - y, loc2.getX() - x);
		return Math.toDegrees(radians) + 90;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public void setY(float y) {
		this.y = y;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
}
