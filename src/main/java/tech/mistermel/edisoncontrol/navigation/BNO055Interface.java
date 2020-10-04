package tech.mistermel.edisoncontrol.navigation;

public class BNO055Interface implements MagnetometerInterface {

	@Override
	public void initialize(float declinationAngle) throws Exception {
		
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
