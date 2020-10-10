package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONObject;

public class TelemetryPacket implements Packet {

	public static final String PACKET_NAME = "telemetry";
	
	private double voltage, temperature;
	
	public TelemetryPacket(double voltage, double temperature) {
		this.voltage = voltage;
		this.temperature = temperature;
	}
	
	@Override
	public void send(JSONObject json) {
		json.put("voltage", voltage);
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
