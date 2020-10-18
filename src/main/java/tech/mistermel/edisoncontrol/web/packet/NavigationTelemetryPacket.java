package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONArray;
import org.json.JSONObject;

import tech.mistermel.edisoncontrol.navigation.Location;

public class NavigationTelemetryPacket implements Packet {

	public static final String PACKET_NAME = "nav";
	
	private Location currentLoc;
	private int heading;
	
	private int targetIndex;
	
	private float[] acceleration;
	private float cte, steeringFactor;
	private int steer, speed;
	
	public NavigationTelemetryPacket(Location currentLoc, int heading, int targetIndex, float[] acceleration, float cte, float steeringFactor, int steer, int speed) {
		this.currentLoc = currentLoc;
		this.heading = heading;
		
		this.targetIndex = targetIndex;
		
		this.acceleration = acceleration;
		this.cte = cte;
		this.steeringFactor = steeringFactor;
		this.steer = steer;
		this.speed = speed;
	}
	
	@Override
	public void send(JSONObject json) {
		JSONArray posJson = new JSONArray();
		json.put("pos", posJson);
		posJson.put(currentLoc.getX());
		posJson.put(currentLoc.getY());
		posJson.put(heading);
		
		json.put("t", targetIndex);
		
		if(acceleration != null) {
			JSONArray accJson = new JSONArray(acceleration);
			json.put("acc", accJson);
		}
		
		JSONArray paramsJson = new JSONArray();
		json.put("params", paramsJson);
		paramsJson.put(cte);
		paramsJson.put(steeringFactor);
		paramsJson.put(steer);
		paramsJson.put(speed);
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
