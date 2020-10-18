package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONArray;
import org.json.JSONObject;

public class NavigationTelemetryPacket implements Packet {

	public static final String PACKET_NAME = "nav";
	
	private float x, y;
	private float[] acceleration;
	private int heading, targetIndex;
	private float cte;
	
	public NavigationTelemetryPacket(float x, float y, int heading, float[] acceleration, int targetIndex, float cte) {
		this.x = x;
		this.y = y;
		this.heading = heading;
		this.acceleration = acceleration;
		this.targetIndex = targetIndex;
		this.cte = cte;
	}
	
	@Override
	public void send(JSONObject json) {
		json.put("x", x);
		json.put("y", y);
		json.put("h", heading);
		json.put("t", targetIndex);
		
		if(acceleration != null) {
			JSONArray accJson = new JSONArray(acceleration);
			json.put("acc", accJson);
		}
		
		JSONArray paramsJson = new JSONArray();
		json.put("params", paramsJson);
		paramsJson.put(cte);
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
