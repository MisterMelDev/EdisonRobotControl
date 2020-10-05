package tech.mistermel.edisoncontrol.navigation;

import org.json.JSONObject;

public interface MagnetometerInterface {

	public void initialize(JSONObject settings);
	public float getHeading();
	public float[] getRawData();
	
}
