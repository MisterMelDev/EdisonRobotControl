package tech.mistermel.edisoncontrol.navigation.magnetometer;

import org.json.JSONObject;

public interface MagnetometerInterface {

	public boolean initialize(JSONObject settings);
	public float getHeading();
	
}
