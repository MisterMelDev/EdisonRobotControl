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
	
	public int directionTo(Location loc2, int heading) {
		double headingDistance = this.getHeadingDistance(heading, (int) this.headingTo(loc2));
		return headingDistance < 0 ? -1 : 1;
	}
	
	private double getHeadingDistance(int a, int b) {
		double left = a - b;
		double right = b - a;
		if(left < 0) left += 360;
		if(right < 0) right += 360;
		return left < right ? -left : right;
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
