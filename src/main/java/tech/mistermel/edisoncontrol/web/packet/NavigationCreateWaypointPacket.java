package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.EdisonControl;

public class NavigationCreateWaypointPacket implements Packet {

	public static final String PACKET_NAME = "nav_createwaypoint";
	
	@Override
	public void send(JSONObject json) {
		// This packet is incoming only
	}

	@Override
	public void receive(JSONObject json) {
		EdisonControl.getInstance().getNavHandler().createWaypoint(json.optFloat("x"), json.optFloat("y"));
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
