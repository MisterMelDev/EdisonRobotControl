package tech.mistermel.edisoncontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.navigation.NavigationHandler;
import tech.mistermel.edisoncontrol.serial.DWMSerialInterface;
import tech.mistermel.edisoncontrol.serial.SerialInterface;
import tech.mistermel.edisoncontrol.web.WebHandler;
import tech.mistermel.edisoncontrol.web.WiFiConfigurationsRoute;

public class EdisonControl {

	private static final Logger logger = LoggerFactory.getLogger(EdisonControl.class);
	
	private SerialInterface serialInterface;
	private DWMSerialInterface dwmSerialInterface;
	private WebHandler webHandler;
	private ConfigHandler configHandler;
	private ProcessHandler processHandler;
	private WiFiHandler wifiHandler;
	private NavigationHandler navHandler;
	
	public void start() {
		this.configHandler = new ConfigHandler();
		configHandler.load();
		
		this.processHandler = new ProcessHandler();
		this.wifiHandler = new WiFiHandler();
		wifiHandler.load();
		
		this.serialInterface = new SerialInterface();
		serialInterface.start();
		
		this.dwmSerialInterface = new DWMSerialInterface();
		dwmSerialInterface.start();
		
		this.webHandler = new WebHandler(configHandler.getJson().optInt("web_port", 8888));
		webHandler.registerRoute("/wifiConfigs", new WiFiConfigurationsRoute());
		webHandler.startWeb();
		
		this.navHandler = new NavigationHandler();
		
		Runtime.getRuntime().addShutdownHook(new Thread("ShutdownThread") {
			@Override
			public void run() {
				logger.info("Shutting down");
				processHandler.stopStreamProcess();
				processHandler.stopLightingProcess();
			}
		});
		
		long timePassed = System.currentTimeMillis() - startupTime;	
		logger.info("Startup completed (took {}ms)", timePassed);
	}
	
	public SerialInterface getSerialInterface() {
		return serialInterface;
	}
	
	public DWMSerialInterface getDWMSerialInterface() {
		return dwmSerialInterface;
	}
	
	public WebHandler getWebHandler() {
		return webHandler;
	}
	
	public ConfigHandler getConfigHandler() {
		return configHandler;
	}
	
	public ProcessHandler getProcessHandler() {
		return processHandler;
	}
	
	public WiFiHandler getWifiHandler() {
		return wifiHandler;
	}
	
	public NavigationHandler getNavHandler() {
		return navHandler;
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
