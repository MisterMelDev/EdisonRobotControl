package tech.mistermel.edisoncontrol.web.packet;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.navigation.Location;
import tech.mistermel.edisoncontrol.navigation.Waypoint;

public class NavigationWaypointsPacket implements Packet {

	public static final String PACKET_NAME = "nav_waypoints";
	private static final Logger logger = LoggerFactory.getLogger(NavigationWaypointsPacket.class);
	
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
			Location waypointLoc = waypoint.getLocation();
			
			JSONObject waypointJson = new JSONObject();
			waypointsJson.put(Integer.toString(i), waypointJson);
			
			waypointJson.put("x", waypointLoc.getX());
			waypointJson.put("y", waypointLoc.getY());
			waypointJson.put("isTarget", waypoint == targetWaypoint);
		}
	}

	@Override
	public void receive(JSONObject json) {
		int index = json.optInt("index");
		float x = json.optFloat("x");
		float y = json.optFloat("y");
		
		List<Waypoint> waypoints = EdisonControl.getInstance().getNavHandler().getWaypoints();
		if(index > waypoints.size() - 1) {
			logger.warn("Cannot move waypoint (index {}) because it does not exist", index);
			return;
		}
		
		EdisonControl.getInstance().getNavHandler().moveWaypoint(waypoints.get(index), x, y);
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
