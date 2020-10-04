package tech.mistermel.edisoncontrol.navigation;

import org.json.JSONObject;

public interface MagnetometerInterface {

	public void initialize(JSONObject settings) throws Exception;
	public float getHeading() throws Exception;
	public float[] getRawData() throws Exception;
	
}
