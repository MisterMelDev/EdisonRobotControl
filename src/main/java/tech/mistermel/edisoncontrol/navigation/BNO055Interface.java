package tech.mistermel.edisoncontrol.navigation;

import org.json.JSONObject;

public class BNO055Interface implements MagnetometerInterface {

	@Override
	public void initialize(JSONObject settings) throws Exception {
		
	}

	@Override
	public float getHeading() throws Exception {
		return 0;
	}

	@Override
	public float[] getRawData() throws Exception {
		return null;
	}

}
