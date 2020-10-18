package tech.mistermel.edisoncontrol.navigation.magnetometer;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatus;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatusType;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Monitorable;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;
import tech.mistermel.edisoncontrol.web.packet.IMUCalibrationPacket;

public class BNO055Interface implements IMUInterface, Monitorable {

	private static final Logger logger = LoggerFactory.getLogger(BNO055Interface.class);
	
	private static final int CHIP_ID = 0xA0;
	
	private static final int BUS_NUM = I2CBus.BUS_1;
	private static final byte DEVICE_ADDR = 0x28;
	
	private static final byte CHIP_ID_ADDR = 0x00;
	private static final byte OPR_MODE_ADDR = 0x3D;
	private static final byte SYS_TRIGGER_ADDR = 0x3F;
	
	private static final byte EULER_DATA_ADDR = 0x1A;
	private static final byte ACC_DATA_ADDR = 0x08;
	private static final byte CALIB_STAT_ADDR = 0x35;
	
	private I2CDevice device;
	private float offset;
	
	@Override
	public boolean initialize(JSONObject settings) {
		this.offset = settings.optFloat("offset", 0);
		
		try {
			I2CBus bus = I2CFactory.getInstance(BUS_NUM);
			this.device = bus.getDevice(DEVICE_ADDR);
			
			this.waitForSensor();
			device.write(OPR_MODE_ADDR, (byte) 0x09); // COMPASS mode
			device.write(SYS_TRIGGER_ADDR, (byte) 0x80);
			
			logger.info("BNO055 initialized");
			EdisonControl.getInstance().getSystemHealthHandler().registerMonitorable(Service.BNO055, this);
			return true;
		} catch(UnsupportedBusNumberException | IOException e) {
			logger.error("Error while attempting to initialize BNO055 - is I2C enabled in raspi-config?");
			return false;
		}
	}
	
	private void waitForSensor() throws IOException {
		try {
			logger.debug("Waiting for sensor...");
			
			while(device.read(CHIP_ID_ADDR) != CHIP_ID) {
				Thread.sleep(100);
			}
			
			logger.debug("Sensor is present");
		} catch (InterruptedException e) {
			logger.error("Interrupted while waiting for sensor", e);
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	public SystemStatus getStatus() {
		if(device == null) {
			return null;
		}
		
		try {
			int calibrationStatus = device.read(CALIB_STAT_ADDR);
			int sysCalib = (calibrationStatus >> 6) & 0x03;
			int gyroCalib = (calibrationStatus >> 4) & 0x03;
			int accCalib = (calibrationStatus >> 2) & 0x03;
			int magCalib = (calibrationStatus >> 0) & 0x03;
			
			return new SystemStatus(sysCalib, gyroCalib, accCalib, magCalib);
		} catch(IOException e) {
			logger.error("Error occurred while attempting to read status from BNO055", e);
			return null;
		}
	}

	@Override
	public float getHeading() {
		try {
			byte[] buffer = new byte[6];
			device.read(EULER_DATA_ADDR, buffer, 0, buffer.length);
			
			float heading = ((buffer[0] & 0xFF) | ((buffer[1] << 8) & 0xFF00)) / 16.0f;
			heading += offset;
			
			heading %= 360;
			return heading;
		} catch (IOException e) {
			logger.error("Error occurred while attempting to read heading from BNO055", e);
			return -1;
		}
	}
	
	@Override
	public float[] getAcceleration() {
		try {
			byte[] buffer = new byte[6];
			device.read(ACC_DATA_ADDR, buffer, 0, buffer.length);
			
			float x = ((buffer[0] & 0xFF) | ((buffer[1] << 8) & 0xFF00)) / 100.0f;
			float y = ((buffer[2] & 0xFF) | ((buffer[3] << 8) & 0xFF00)) / 100.0f;
			float z = ((buffer[4] & 0xFF) | ((buffer[5] << 8) & 0xFF00)) / 100.0f;
			
			return new float[] { x, y, z };
		} catch (IOException e) {
			logger.error("Error occurred while attempting to read accelerometer data from BNO055", e);
			return null;
		}
	}

	@Override
	public boolean isWorking() {
		SystemStatus status = this.getStatus();
		
		IMUCalibrationPacket packet = new IMUCalibrationPacket(new int[] {status.getSysCalib(), status.getGyroCalib(), status.getAccCalib(), status.getMagCalib()});
		EdisonControl.getInstance().getWebHandler().sendPacket(packet);
		
		return status.isCompletelyCalibrated();
	}

	@Override
	public HealthStatus getResultingStatus() {
		return new HealthStatus(HealthStatusType.REQUIRES_ATTENTION, "Not (completely) calibrated");
	}

}
