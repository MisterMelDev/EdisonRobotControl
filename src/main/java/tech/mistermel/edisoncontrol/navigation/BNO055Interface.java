package tech.mistermel.edisoncontrol.navigation;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class BNO055Interface implements MagnetometerInterface {

	private static final Logger logger = LoggerFactory.getLogger(BNO055Interface.class);
	
	private static final int CHIP_ID = 0xA0;
	
	private static final int BUS_NUM = I2CBus.BUS_1;
	private static final byte DEVICE_ADDR = 0x28;
	
	private static final byte CHIP_ID_ADDR = 0x00;
	private static final byte EULER_REGISTER_ADDR = 0x1A;
	
	private I2CDevice device;
	private boolean isSensorPresent;
	
	@Override
	public void initialize(JSONObject settings) {
		try {
			I2CBus bus = I2CFactory.getInstance(BUS_NUM);
			this.device = bus.getDevice(DEVICE_ADDR);
			
			logger.info("BNO055 initialized");
		} catch(UnsupportedBusNumberException | IOException e) {
			logger.error("Error while attempting to initialize BNO055 - is I2C enabled in raspi-config?");
		}
	}

	@Override
	public float getHeading() {
		try {
			if(!isSensorPresent) {
				if(device.read(CHIP_ID_ADDR) == CHIP_ID) {
					this.isSensorPresent = true;
					logger.info("BNO055 connected");
				} else {
					logger.debug("Sensor is not present");
					return 0;
				}
			}
			
			byte[] buffer = new byte[6];
			device.read(EULER_REGISTER_ADDR, buffer, 0, buffer.length);
			
			double x = ((buffer[0] & 0xFF) | ((buffer[1] << 8) & 0xFF00)) / 16.0;
			double y = ((buffer[2] & 0xFF) | ((buffer[3] << 8) & 0xFF00)) / 16.0;
			double z = ((buffer[4] & 0xFF) | ((buffer[5] << 8) & 0xFF00)) / 16.0;
			
			logger.info("x: {}  y: {}  z: {}", x, y, z);
			
			// To be implemented
			return 0;
		} catch (IOException e) {
			logger.error("Error occurred while attempting to read heading from BNO055", e);
			return -1;
		}
	}
	
	public static void test() {
		BNO055Interface intf = new BNO055Interface();
		intf.initialize(null);
		
		while(true) {
			intf.getHeading();
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
