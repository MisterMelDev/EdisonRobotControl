package tech.mistermel.edisoncontrol.web.packet;

import java.util.List;

import org.json.JSONObject;

import tech.mistermel.edisoncontrol.navigation.Waypoint;

public class NavigationWaypointsPacket implements Packet {

	public static final String PACKET_NAME = "nav_waypoints";
	
	private List<Waypoint> waypoints;
	private Waypoint targetWaypoint;
	
	public NavigationWaypointsPacket(List<Waypoint> waypoints, Waypoint targetWaypoint) {
		this.waypoints = waypoints;
		this.targetWaypoint = targetWaypoint;
	}

	@Override
	public void send(JSONObject json) {
		JSONObject waypointsJson = new JSONObject();
		json.put("waypoints", waypointsJson);
		
		waypointsJson.put("size", waypoints.size());
		for(int i = 0; i < waypoints.size(); i++) {
			Waypoint waypoint = waypoints.get(i);
			
			JSONObject waypointJson = new JSONObject();
			waypointsJson.put(Integer.toString(i), waypointJson);
			
			waypointJson.put("x", waypoint.getX());
			waypointJson.put("y", waypoint.getY());
			waypointJson.put("isTarget", waypoint == targetWaypoint);
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
