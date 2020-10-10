package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.EdisonControl;

public class SystemCommandPacket implements Packet {

	public static final String PACKET_NAME = "system_cmd";
	
	@Override
	public void send(JSONObject json) {
		// This packet is incoming only
	}

	@Override
	public void receive(JSONObject json) {
		String action = json.optString("action");
		
		if(action.equals("shutdown")) {
			EdisonControl.getInstance().getProcessHandler().shutdown();
			return;
		}
		
		if(action.equals("reboot")) {
			EdisonControl.getInstance().getProcessHandler().reboot();
			return;
		}
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
