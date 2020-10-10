package tech.mistermel.edisoncontrol.web.packet;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;

public class WiFiPacket implements Packet {

	private static final Logger logger = LoggerFactory.getLogger(WiFiPacket.class);
	public static final String PACKET_NAME = "wifi";
	
	@Override
	public void send(JSONObject json) {
		// This packet is incoming packet
	}

	@Override
	public void receive(JSONObject json) {
		try {
			EdisonControl.getInstance().getWifiHandler().setNetwork(json.optString("ssid"), json.optString("password"));
			EdisonControl.getInstance().getProcessHandler().reboot();
		} catch (IOException e) {
			logger.error("Error occured while attempting to set WiFi network", e);
		}
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
