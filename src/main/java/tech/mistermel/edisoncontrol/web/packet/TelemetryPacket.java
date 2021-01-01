package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

public class TelemetryPacket implements Packet {

	public static final String PACKET_NAME = "telemetry";
	
	private double voltageMain, voltageSmall, temperature;
	
	public TelemetryPacket(double voltageMain, double voltageSmall, double temperature) {
		this.voltageMain = voltageMain;
		this.voltageSmall = voltageSmall;
		this.temperature = temperature;
	}
	
	@Override
	public void send(JSONObject json) {
		JSONObject voltageObj = new JSONObject();
		json.put("voltage", voltageObj);
		voltageObj.put("main", voltageMain);
		voltageObj.put("small", voltageSmall);
		
		json.put("temp", temperature);
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
