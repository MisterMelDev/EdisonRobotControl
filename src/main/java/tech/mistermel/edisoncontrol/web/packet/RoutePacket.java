package tech.mistermel.edisoncontrol.web.packet;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import tech.mistermel.edisoncontrol.navigation.Location;

public class RoutePacket implements Packet {

	public static final String PACKET_NAME = "route";
	
	private List<Location> curvePoints;
	
	public RoutePacket(List<Location> curvePoints) {
		this.curvePoints = curvePoints;
	}
	
	@Override
	public void send(JSONObject json) {
		JSONArray curvePointsJson = new JSONArray();
		json.put("curve_points", curvePointsJson);
		
		for(Location curvePoint : curvePoints) {
			JSONArray curvePointJson = new JSONArray();
			curvePointJson.put(curvePoint.getX());
			curvePointJson.put(curvePoint.getY());
			
			curvePointsJson.put(curvePointJson);
		}
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
