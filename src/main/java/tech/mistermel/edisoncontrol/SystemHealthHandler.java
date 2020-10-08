package tech.mistermel.edisoncontrol;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.web.WebHandler;

public class SystemHealthHandler {

	private static final Logger logger = LoggerFactory.getLogger(SystemHealthHandler.class);
	
	public enum HealthStatus {
		UNKNOWN, DISABLED, INITIALIZING, RUNNING, STOPPING, FAULT;	
	}
	
	public enum Service {
		SERIAL_MOBO("Motherboard Serial"), SERIAL_DWM("DWM1001 Serial"), BNO055("BNO055 IMU");
		
		private String displayName;
		
		private Service(String displayName) {
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}
	}
	
	private Map<Service, HealthStatus> statuses = new EnumMap<>(Service.class);
	
	public SystemHealthHandler() {
		for(Service service : Service.values()) {
			statuses.put(service, HealthStatus.UNKNOWN);
		}
	}
	
	private void sendPacket() {
		JSONObject json = new JSONObject();
		json.put("type", "system_health");
		
		JSONObject servicesJson = new JSONObject();
		json.put("services", servicesJson);
		
		for(Entry<Service, HealthStatus> entry : statuses.entrySet()) {
			JSONObject serviceJson = new JSONObject();
			serviceJson.put("name", entry.getKey().getDisplayName());
			serviceJson.put("status", entry.getValue().name());
			
			servicesJson.put(entry.getKey().name(), serviceJson);
		}
		
		WebHandler webHandler = EdisonControl.getInstance().getWebHandler();
		if(webHandler != null) {
			webHandler.sendPacket(json);
		}
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
	
}
