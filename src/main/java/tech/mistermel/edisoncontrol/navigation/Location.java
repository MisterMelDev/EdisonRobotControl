package tech.mistermel.edisoncontrol.navigation;

public class Location {

	private float x;
	private float y;
	
	public Location() {}
	
	public Location(float x, float y) {
		this.x = x;
		this.y = y;
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
