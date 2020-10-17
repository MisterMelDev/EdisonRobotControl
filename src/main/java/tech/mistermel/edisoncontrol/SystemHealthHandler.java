package tech.mistermel.edisoncontrol;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public interface Monitorable {
		public boolean isWorking();
		public HealthStatus getResultingStatus();
	}
	
	private Map<Service, HealthStatus> statuses = new EnumMap<>(Service.class);
	private Map<Service, Monitorable> monitorables = new HashMap<>();
	
	public SystemHealthHandler() {
		for(Service service : Service.values()) {
			statuses.put(service, new HealthStatus(service.getDefaultStatus()));
		}
	}
	
	public void registerMonitorable(Service service, Monitorable monitorable) {
		monitorables.put(service, monitorable);
		logger.debug("New monitorable registered for {}: {}", service.name(), monitorable.getClass().getName());
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
		
		public SystemHealthMonitorThread() {
			super("SystemHealthMonitorThread");
		}
		
		@Override
		public void run() {
			while(true) {	
				try {
					for(Entry<Service, Monitorable> entry : monitorables.entrySet()) {
						Service service = entry.getKey();
						HealthStatusType currentHealth = getStatus(service);
						
						Monitorable monitorable = entry.getValue();
						boolean isWorking = monitorable.isWorking();
						HealthStatus resultingStatus = monitorable.getResultingStatus();
						
						if(!isWorking && currentHealth == HealthStatusType.RUNNING) {
							logger.warn("Service {} is not working, switching to {} state", service.name(), resultingStatus.getType().name());
							setStatus(service, monitorable.getResultingStatus());
						} else if(isWorking && currentHealth == resultingStatus.getType()) {
							logger.info("Service {} is working normally again, switching to RUNNING state");
							setStatus(service, HealthStatusType.RUNNING);
						}
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
