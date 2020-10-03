package tech.mistermel.edisoncontrol.navigation.calibrate;

import tech.mistermel.edisoncontrol.navigation.HMC5883LInterface;

public class CalibrateLauncher {

	public static void calibrate() {
		HMC5883LInterface intf = new HMC5883LInterface();
		
		final int seconds = 30;
		
		float xMax = 0, yMax = 0, xMin = 0, yMin = 0;
		long finishTime = System.currentTimeMillis() + 30 * 1000;
		
		try {
			intf.initialize(0);
			
			System.out.println("Now starting calibration. Move the magnetometer in circles. Calibration will run for " + seconds + " seconds");
			while(System.currentTimeMillis() < finishTime) {
				float[] rawData = intf.getRawData();
				
				if(rawData[0] < xMin) xMin = rawData[0];
				if(rawData[0] > xMax) xMax = rawData[0];
				if(rawData[1] < yMin) yMin = rawData[1];
				if(rawData[1] > yMax) yMax = rawData[1];
			}
			
			float offX = (xMax + xMin) / 2;
			float offY = (yMax + yMin) / 2;
			
			System.out.println("X: " + xMin + "-" + xMax + " == " + offX);
			System.out.println("Y: " + yMin + "-" + yMax + " == " + offY);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
