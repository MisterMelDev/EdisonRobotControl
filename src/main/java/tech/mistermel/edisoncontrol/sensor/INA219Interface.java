package tech.mistermel.edisoncontrol.sensor;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class INA219Interface {

	private static final Logger logger = LoggerFactory.getLogger(INA219Interface.class);
	
	private static final int BUS_NUM = I2CBus.BUS_1;
	private static final byte DEVICE_ADDR = 0x40;
	
	private static final int BUS_VOLTAGE_ADDR = 0x02;
	
	private I2CDevice device;
	
	public INA219Interface() {
		try {
			I2CBus bus = I2CFactory.getInstance(BUS_NUM);
			this.device = bus.getDevice(DEVICE_ADDR);
		} catch(UnsupportedBusNumberException | IOException e) {
			logger.error("Error while attempting to initialize INA219 - is I2C enabled in raspi-config?");
		}
	}
	
	public double readVoltage() {
		if(device == null)
			return 0.0;
		
		try {
			byte[] buffer = new byte[2];
			device.read(BUS_VOLTAGE_ADDR, buffer, 0, buffer.length);
			int val = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
			
			return (val >> 3) * 4e-3;
		} catch (IOException e) {
			logger.error("Error while attempting to read bus voltage", e);
			return -1;
		}
	}
	
}
