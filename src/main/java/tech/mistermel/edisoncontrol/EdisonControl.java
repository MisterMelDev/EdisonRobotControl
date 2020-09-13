package tech.mistermel.edisoncontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.web.WebHandler;

public class EdisonControl {

	private static final Logger logger = LoggerFactory.getLogger(EdisonControl.class);
	
	private SerialInterface serialInterface;
	private WebHandler webHandler;
	
	public EdisonControl() {
		this.serialInterface = new SerialInterface();
		this.webHandler = new WebHandler();
	}
	
	public void start() {
		serialInterface.start();
		webHandler.start();

		long timePassed = System.currentTimeMillis() - startupTime;
		logger.info("Startup completed (took {}ms)", timePassed);
	}
	
	public SerialInterface getSerialInterface() {
		return serialInterface;
	}
	
	public WebHandler getWebHandler() {
		return webHandler;
	}
	
	private static EdisonControl instance;
	private static long startupTime;
	
	public static void main(String[] args) {
		startupTime = System.currentTimeMillis();
		instance = new EdisonControl();
		instance.start();
	}
	
	public static EdisonControl getInstance() {
		return instance;
	}
	
}
