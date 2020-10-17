package tech.mistermel.edisoncontrol.web.packet;

import org.json.JSONArray;
import org.json.JSONObject;

public class IMUCalibrationPacket implements Packet {

	public static final String PACKET_NAME = "imu_calib";
	
	private int[] calibData;
	
	public IMUCalibrationPacket(int[] calibData) {
		this.calibData = calibData;
	}
	
	@Override
	public void send(JSONObject json) {
		json.put("calibStatuses", new JSONArray(calibData));
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
