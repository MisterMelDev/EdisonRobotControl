package tech.mistermel.edisoncontrol.navigation.magnetometer;

public class SystemStatus {

	private int sysCalib, gyroCalib, accCalib, magCalib;
	
	public SystemStatus(int sysCalib, int gyroCalib, int accCalib, int magCalib) {
		this.sysCalib = sysCalib;
		this.gyroCalib = gyroCalib;
		this.accCalib = accCalib;
		this.magCalib = magCalib;
	}
	
	public boolean isCompletelyCalibrated() {
		return sysCalib == 3 && gyroCalib == 3 && accCalib == 3 && magCalib == 3;
	}
	
	public int getSysCalib() {
		return sysCalib;
	}
	
	public int getGyroCalib() {
		return gyroCalib;
	}
	
	public int getAccCalib() {
		return accCalib;
	}
	
	public int getMagCalib() {
		return magCalib;
	}
	
}
