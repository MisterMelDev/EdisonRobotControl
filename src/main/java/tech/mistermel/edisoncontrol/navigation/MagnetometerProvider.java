package tech.mistermel.edisoncontrol.navigation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;

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
			intf.initialize();
			while(true) {
				float heading = intf.getHeading();
				EdisonControl.getInstance().getNavHandler().onHeadingReceived(heading);
				
				Thread.sleep(100);
			}
		} catch(Exception e) {
			logger.error("Error occurred in magnetometer reading thread", e);
		}
	}
	
}
