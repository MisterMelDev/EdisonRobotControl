package tech.mistermel.edisoncontrol;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.serial.DWMSerialInterface;
import tech.mistermel.edisoncontrol.serial.SerialInterface;
import tech.mistermel.edisoncontrol.web.WebHandler;
import tech.mistermel.edisoncontrol.web.packet.SystemHealthPacket;

public class SystemHealthHandler {

	private static final Logger logger = LoggerFactory.getLogger(SystemHealthHandler.class);
	
	public enum HealthStatus {
		UNKNOWN, DISABLED, INITIALIZING, RUNNING, STOPPING, FAULT;	
	}
	
	public enum Service {
		SERIAL_MOBO("Motherboard Serial"), SERIAL_DWM("DWM1001 Serial"), BNO055("BNO055 IMU"),
		STREAM("Stream", HealthStatus.DISABLED), NAVIGATION("Navigation", HealthStatus.DISABLED);
		
		private String displayName;
		private HealthStatus defaultStatus;
		
		private Service(String displayName) {
			this.displayName = displayName;
			this.defaultStatus = HealthStatus.UNKNOWN;
		}
		
		private Service(String displayName, HealthStatus defaultStatus) {
			this.displayName = displayName;
			this.defaultStatus = defaultStatus;
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		public HealthStatus getDefaultStatus() {
			return defaultStatus;
		}
	}
	
	private Map<Service, HealthStatus> statuses = new EnumMap<>(Service.class);
	
	public SystemHealthHandler() {
		for(Service service : Service.values()) {
			statuses.put(service, service.getDefaultStatus());
		}
	}
	
	public void onStartupComplete() {
		new SystemHealthMonitorThread().start();
	}
	
	public void sendPacket() {
		WebHandler webHandler = EdisonControl.getInstance().getWebHandler();
		if(webHandler == null) {
			return;
		}
		
		SystemHealthPacket packet = new SystemHealthPacket(statuses);
		webHandler.sendPacket(packet);
	}
	
	public void setStatus(Service service, HealthStatus status) {
		logger.debug("New status for service {}: {}", service.name(), status.name());
		
		statuses.put(service, status);
		this.sendPacket();
	}
	
	public HealthStatus getStatus(Service service) {
		return statuses.get(service);
	}
	
	public Map<Service, HealthStatus> getStatuses() {
		return statuses;
	}
	
	public class SystemHealthMonitorThread extends Thread {
		
		@Override
		public void run() {
			SerialInterface serialInterface = EdisonControl.getInstance().getSerialInterface();
			DWMSerialInterface dwmSerialInterface = EdisonControl.getInstance().getDWMSerialInterface();
			
			while(true) {	
				try {
					HealthStatus moboStatus = getStatus(Service.SERIAL_MOBO);
					if(moboStatus != HealthStatus.FAULT && !serialInterface.isCommunicationWorking()) {
						logger.warn("Motherboard serial communication not working, setting FAULT state");
						setStatus(Service.SERIAL_MOBO, HealthStatus.FAULT);
					} else if(moboStatus == HealthStatus.FAULT && serialInterface.isCommunicationWorking()) {
						logger.warn("Motherboard serial communication working normally, setting RUNNING state");
						setStatus(Service.SERIAL_MOBO, HealthStatus.RUNNING);
					}
					
					HealthStatus dwmStatus = getStatus(Service.SERIAL_DWM);
					if(dwmStatus != HealthStatus.FAULT && !dwmSerialInterface.isCommunicationWorking()) {
						logger.warn("DWM serial communication not working, setting FAULT state");
						setStatus(Service.SERIAL_DWM, HealthStatus.FAULT);
					} else if(dwmStatus == HealthStatus.FAULT && dwmSerialInterface.isCommunicationWorking()) {
						logger.warn("DWM serial communication working normally, setting RUNNING state");
						setStatus(Service.SERIAL_DWM, HealthStatus.RUNNING);
					}
					
					Thread.sleep(500);
				} catch (InterruptedException e) {
					logger.error("System health monitor thread interrupted", e);
					Thread.currentThread().interrupt();
				}
			}
		}
		
	}
	
}
