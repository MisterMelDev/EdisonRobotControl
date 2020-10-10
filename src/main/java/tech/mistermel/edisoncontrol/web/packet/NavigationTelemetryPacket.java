package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

public class NavigationTelemetryPacket implements Packet {

	public static final String PACKET_NAME = "nav";
	
	private float x, y, d;
	private int heading, targetHeading;
	
	public NavigationTelemetryPacket(float x, float y, float d, int heading, int targetHeading) {
		this.x = x;
		this.y = y;
		this.d = d;
		this.heading = heading;
		this.targetHeading = targetHeading;
	}
	
	@Override
	public void send(JSONObject json) {
		json.put("x", x);
		json.put("y", y);
		json.put("d", d);
		json.put("h", heading);
		json.put("th", targetHeading);
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
