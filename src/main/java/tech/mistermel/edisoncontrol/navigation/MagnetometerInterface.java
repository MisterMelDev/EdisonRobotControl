package tech.mistermel.edisoncontrol.navigation;

public interface MagnetometerInterface {

	public void initialize() throws Exception;
	public float getHeading() throws Exception;
	
}
