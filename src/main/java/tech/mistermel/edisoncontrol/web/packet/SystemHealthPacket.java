package tech.mistermel.edisoncontrol.web.packet;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatus;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;

public class SystemHealthPacket implements Packet {

	public static final String PACKET_NAME = "health";
	
	private Map<Service, HealthStatus> statuses;
	
	public SystemHealthPacket(Map<Service, HealthStatus> statuses) {
		this.statuses = statuses;
	}
	
	@Override
	public void send(JSONObject json) {
		JSONObject servicesJson = new JSONObject();
		json.put("services", servicesJson);
		
		for(Entry<Service, HealthStatus> entry : statuses.entrySet()) {
			servicesJson.put(entry.getKey().getDisplayName(), entry.getValue().name());
		}
	}
	
	@Override
	public void receive(JSONObject json) {
		// This packet is outgoing only
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}
	
}