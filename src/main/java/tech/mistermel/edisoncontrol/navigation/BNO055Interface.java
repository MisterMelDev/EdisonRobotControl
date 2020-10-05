package tech.mistermel.edisoncontrol.navigation;

import org.json.JSONObject;

public class BNO055Interface implements MagnetometerInterface {

	@Override
	public void initialize(JSONObject settings) {
		// To be implemented
	}

	@Override
	public float getHeading() {
		// To be implemented
		return 0;
	}

	@Override
	public float[] getRawData() {
		// To be implemented
		return new float[] { 0.0f, 0.0f, 0.0f };
	}

}
