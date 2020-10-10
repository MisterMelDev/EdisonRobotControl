package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.EdisonControl;

public class HeartbeatPacket implements Packet {

	public static final String PACKET_NAME = "heartbeat";
	
	@Override
	public void send(JSONObject json) {
		// This packet is incoming only
	}

	@Override
	public void receive(JSONObject json) {
		EdisonControl.getInstance().getWebHandler().onHeartbeatReceive();
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
