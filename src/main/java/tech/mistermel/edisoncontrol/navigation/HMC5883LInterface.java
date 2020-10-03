package tech.mistermel.edisoncontrol.navigation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class HMC5883LInterface implements MagnetometerInterface {

	private static final Logger logger = LoggerFactory.getLogger(HMC5883LInterface.class);
	
	private static final int ADDR = 0x1e;
	private static final int MODE_REGISTER = 0x02, GAIN_REGISTER = 0x01, READ_REGISTER = 0x03;
	
	private static final byte GAIN_VALUE = (byte) 0x20;
	private static final float MG_PER_DIGIT = 0.92f;
	
	private I2CDevice device;
	private double declinationAngle;

	@Override
	public void initialize(float declinationAngle) throws Exception {
		this.declinationAngle = Math.toRadians(declinationAngle);
		
		try {
			this.device = I2CFactory.getInstance(I2CBus.BUS_1).getDevice(ADDR);
			device.write(MODE_REGISTER, (byte) 0x00);
			device.write(GAIN_REGISTER, GAIN_VALUE);
			
			logger.debug("HMC5883L interface initialized");
		} catch(UnsupportedBusNumberException e) {
			logger.warn("Failed to get I2C bus #1, is I2C enabled in raspi-config?");
			logger.warn("HMC5883L initialization failed");
		}
	}
	
	@Override
	public float[] getRawData() throws Exception {
		byte[] data = new byte[6];
		device.read(READ_REGISTER, data, 0, 6);

		int xMag = ((data[0] & 0xFF) * 256 + (data[1] & 0xFF));
		if (xMag > 32767) {
			xMag -= 65536;
		}

		int zMag = ((data[2] & 0xFF) * 256 + (data[3] & 0xFF));
		if (zMag > 32767) {
			zMag -= 65536;
		}

		int yMag = ((data[4] & 0xFF) * 256 + (data[5] & 0xFF));
		if (yMag > 32767) {
			yMag -= 65536;
		}
		
		xMag *= MG_PER_DIGIT;
		yMag *= MG_PER_DIGIT;
		zMag *= MG_PER_DIGIT;
		
		return new float[] {xMag, yMag, zMag};
	}

	@Override
	public float getHeading() throws Exception {
		if(device == null) {
			return 0;
		}
		
		float[] rawData = this.getRawData();
		float xMag = rawData[0];
		float yMag = rawData[1];
		
		float heading = (float) Math.atan2(yMag, xMag);
		heading += declinationAngle; // This was converted into radians above
		
		if(heading < 0)
			heading += 2 * Math.PI;
		else if(heading > 2 * Math.PI)
			heading -= 2 * Math.PI;
		
		float degrees = (float) Math.toDegrees(heading);
		return degrees;
	}

}
