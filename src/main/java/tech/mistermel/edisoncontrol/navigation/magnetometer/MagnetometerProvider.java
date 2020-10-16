package tech.mistermel.edisoncontrol.navigation.magnetometer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatusType;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;

public class MagnetometerProvider extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(MagnetometerProvider.class);
	
	private MagnetometerInterface intf;
	
	public MagnetometerProvider(MagnetometerInterface intf) {
		super("MagnetometerThread");
		this.intf = intf;
	}
	
	@Override
	public void run() {
		try {
			JSONObject config = EdisonControl.getInstance().getConfigHandler().getJson().optJSONObject("compass");
			
			EdisonControl.setStatus(Service.BNO055, HealthStatusType.INITIALIZING);
			if(!intf.initialize(config)) {
				EdisonControl.setStatus(Service.BNO055, HealthStatusType.FAULT, "Init failed");
				
				logger.warn("Magnetometer initialization failed, magnetometer provider thread exiting");
				EdisonControl.getInstance().getNavHandler().onHeadingReceived(0);
				return;
			}
			
			EdisonControl.setStatus(Service.BNO055, HealthStatusType.RUNNING);
			while(true) {
				float heading = intf.getHeading();
				EdisonControl.getInstance().getNavHandler().onHeadingReceived(heading);
				
				Thread.sleep(100);
			}
		} catch(Exception e) {
			logger.error("Error occurred in magnetometer reading thread", e);
			EdisonControl.setStatus(Service.BNO055, HealthStatusType.FAULT, "Error in reading thread");
		}
	}
	
	public SystemStatus getStatus() {
		return intf.getStatus();
	}
	
}
