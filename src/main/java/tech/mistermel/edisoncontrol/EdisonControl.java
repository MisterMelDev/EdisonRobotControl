package tech.mistermel.edisoncontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.web.WebHandler;

public class EdisonControl {

	private static final Logger logger = LoggerFactory.getLogger(EdisonControl.class);
	
	private SerialInterface serialInterface;
	private WebHandler webHandler;
	private ConfigHandler configHandler;
	
	public EdisonControl() {
		this.configHandler = new ConfigHandler();
		configHandler.load();
		
		this.serialInterface = new SerialInterface();
		this.webHandler = new WebHandler(configHandler.getJson().optInt("web_port", 8888));
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
	
	public ConfigHandler getConfigHandler() {
		return configHandler;
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
