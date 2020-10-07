package tech.mistermel.edisoncontrol.navigation;

import org.json.JSONObject;

public interface MagnetometerInterface {

	public boolean initialize(JSONObject settings);
	public float getHeading();
	
}
