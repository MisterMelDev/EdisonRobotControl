package tech.mistermel.edisoncontrol.navigation;

public interface MagnetometerInterface {

	public void initialize(float declinationAngle) throws Exception;
	public float getHeading() throws Exception;
	public float[] getRawData() throws Exception;
	
}
