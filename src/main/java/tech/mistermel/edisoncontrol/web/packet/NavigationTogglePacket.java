package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.EdisonControl;

public class NavigationTogglePacket implements Packet {

	public static final String PACKET_NAME = "nav_toggle";
	
	private boolean enabled;
	
	public NavigationTogglePacket() {}
	
	public NavigationTogglePacket(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public void send(JSONObject json) {
		json.put("enabled", enabled);
	}

	@Override
	public void receive(JSONObject json) {
		if(!EdisonControl.getInstance().getNavHandler().setActive(json.optBoolean("enabled"))) {
			this.enabled = false;
			EdisonControl.getInstance().getWebHandler().sendPacket(this);
		}
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
