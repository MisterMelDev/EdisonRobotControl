package tech.mistermel.edisoncontrol.navigation.magnetometer;

import org.json.JSONObject;

public interface IMUInterface {

	public boolean initialize(JSONObject settings);
	public SystemStatus getStatus();
	
	public float getHeading();
	public float[] getAcceleration();
	
}
