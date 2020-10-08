package tech.mistermel.edisoncontrol.navigation.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PositionFilter {

	private static final int NUM_MEASUREMENTS = 3;
	
	private RollingAverage averageX, averageY;
	
	public PositionFilter() {
		this.averageX = new RollingAverage(NUM_MEASUREMENTS);
		this.averageY = new RollingAverage(NUM_MEASUREMENTS);
	}
	
	private float round(float val, int decimalPlaces) {
		BigDecimal bdVal = BigDecimal.valueOf(val);
		bdVal = bdVal.setScale(decimalPlaces, RoundingMode.HALF_UP);
		return bdVal.floatValue();
	}
	
	public void inputPosition(float x, float y) {
		averageX.add(x);
		averageY.add(y);
	}
	
	public float[] getPosition() {
		float x = round(averageX.getAverage(), 2);
		float y = round(averageY.getAverage(), 2);
		
		return new float[] { x, y };
	}
	
}
