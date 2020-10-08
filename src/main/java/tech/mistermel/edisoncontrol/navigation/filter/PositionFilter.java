package tech.mistermel.edisoncontrol.navigation.filter;

public class PositionFilter {

	private static final int NUM_MEASUREMENTS = 5;
	
	private RollingAverage averageX, averageY;
	
	public PositionFilter() {
		this.averageX = new RollingAverage(NUM_MEASUREMENTS);
		this.averageY = new RollingAverage(NUM_MEASUREMENTS);
	}
	
	public void inputPosition(float x, float y) {
		averageX.add(x);
		averageY.add(y);
	}
	
	public float[] getPosition() {
		return new float[] { averageX.getAverage(), averageY.getAverage() };
	}
	
}
