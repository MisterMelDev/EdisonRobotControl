package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

public interface Packet {

	public void send(JSONObject json);
	public void receive(JSONObject json);
	public String getPacketName();
	
}
