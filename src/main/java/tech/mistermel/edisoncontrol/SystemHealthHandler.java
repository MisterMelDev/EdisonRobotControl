package tech.mistermel.edisoncontrol;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.navigation.magnetometer.MagnetometerProvider;
import tech.mistermel.edisoncontrol.navigation.magnetometer.SystemStatus;
import tech.mistermel.edisoncontrol.serial.DWMSerialInterface;
import tech.mistermel.edisoncontrol.serial.SerialInterface;
import tech.mistermel.edisoncontrol.web.WebHandler;
import tech.mistermel.edisoncontrol.web.packet.SystemHealthPacket;

public class SystemHealthHandler {

	private static final Logger logger = LoggerFactory.getLogger(SystemHealthHandler.class);
	
	public enum HealthStatusType {
		UNKNOWN, DISABLED, INITIALIZING, RUNNING, STOPPING, FAULT, REQUIRES_ATTENTION;	
	}
	
	public static class HealthStatus {
		
		private HealthStatusType type;
		private String extraInfo;
		
		public HealthStatus(HealthStatusType type) {
			this.type = type;
		}
		
		public HealthStatus(HealthStatusType type, String extraInfo) {
			this.type = type;
			this.extraInfo = extraInfo;
		}
		
		public HealthStatusType getType() {
			return type;
		}
		
		public boolean hasExtraInfo() {
			return extraInfo != null && !extraInfo.isEmpty();
		}
		
		public String getExtraInfo() {
			return extraInfo;
		}
	}
	
	public enum Service {
		SERIAL_MOBO("Motherboard Serial"), SERIAL_DWM("DWM1001 Serial"), BNO055("BNO055 IMU"),
		STREAM("Stream", HealthStatusType.DISABLED), NAVIGATION("Navigation", HealthStatusType.DISABLED);
		
		private String displayName;
		private HealthStatusType defaultStatus;
		
		private Service(String displayName) {
			this.displayName = displayName;
			this.defaultStatus = HealthStatusType.UNKNOWN;
		}
		
		private Service(String displayName, HealthStatusType defaultStatus) {
			this.displayName = displayName;
			this.defaultStatus = defaultStatus;
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		public HealthStatusType getDefaultStatus() {
			return defaultStatus;
		}
	}
	
	private Map<Service, HealthStatus> statuses = new EnumMap<>(Service.class);
	
	public SystemHealthHandler() {
		for(Service service : Service.values()) {
			statuses.put(service, new HealthStatus(service.getDefaultStatus()));
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
	
	public void setStatus(Service service, HealthStatusType statusType) {
		this.setStatus(service, new HealthStatus(statusType));
	}
	
	public void setStatus(Service service, HealthStatus status) {
		logger.debug("New status for service {}: {} ({})", service.name(), status.getType().name(), status.hasExtraInfo() ? status.getExtraInfo() : "no extra info");
		
		statuses.put(service, status);
		this.sendPacket();
	}
	
	public HealthStatusType getStatus(Service service) {
		return statuses.get(service).getType();
	}
	
	public Map<Service, HealthStatus> getStatuses() {
		return statuses;
	}
	
	public class SystemHealthMonitorThread extends Thread {
		
		@Override
		public void run() {
			SerialInterface serialInterface = EdisonControl.getInstance().getSerialInterface();
			DWMSerialInterface dwmSerialInterface = EdisonControl.getInstance().getDWMSerialInterface();
			MagnetometerProvider magProvider = EdisonControl.getInstance().getNavHandler().getMagnetometerProvider();
			
			while(true) {	
				try {
					HealthStatusType moboStatus = getStatus(Service.SERIAL_MOBO);
					if(moboStatus != HealthStatusType.FAULT && !serialInterface.isCommunicationWorking()) {
						logger.warn("Motherboard serial communication not working, setting FAULT state");
						setStatus(Service.SERIAL_MOBO, new HealthStatus(HealthStatusType.FAULT, "Communication interrupted"));
					} else if(moboStatus == HealthStatusType.FAULT && serialInterface.isCommunicationWorking()) {
						logger.info("Motherboard serial communication working normally, setting RUNNING state");
						setStatus(Service.SERIAL_MOBO, HealthStatusType.RUNNING);
					}
					
					HealthStatusType dwmStatus = getStatus(Service.SERIAL_DWM);
					if(dwmStatus != HealthStatusType.FAULT && !dwmSerialInterface.isCommunicationWorking()) {
						logger.warn("DWM serial communication not working, setting FAULT state");
						setStatus(Service.SERIAL_DWM, new HealthStatus(HealthStatusType.FAULT, "Communication interrupted"));
					} else if(dwmStatus == HealthStatusType.FAULT && dwmSerialInterface.isCommunicationWorking()) {
						logger.info("DWM serial communication working normally, setting RUNNING state");
						setStatus(Service.SERIAL_DWM, HealthStatusType.RUNNING);
					}
					
					HealthStatusType bnoStatus = getStatus(Service.BNO055);
					SystemStatus bnoCalib = magProvider.getStatus();
					if(bnoStatus == HealthStatusType.RUNNING && !bnoCalib.isCompletelyCalibrated()) {
						logger.warn("BNO055 is not completely calibrated, setting REQUIRES_ATTENTION state");
						setStatus(Service.BNO055, new HealthStatus(HealthStatusType.REQUIRES_ATTENTION, "Not fully calibrated"));
					} else if(bnoStatus == HealthStatusType.REQUIRES_ATTENTION && bnoCalib.isCompletelyCalibrated()) {
						logger.info("BNO055 calibration complete, setting RUNNING state");
						setStatus(Service.BNO055, HealthStatusType.RUNNING);
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
