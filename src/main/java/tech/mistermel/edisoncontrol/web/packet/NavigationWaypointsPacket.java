package tech.mistermel.edisoncontrol.web.packet;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.navigation.Waypoint;

public class NavigationWaypointsPacket implements Packet {

	public static final String PACKET_NAME = "nav_waypoints";
	private static final Logger logger = LoggerFactory.getLogger(NavigationWaypointsPacket.class);
	
	private List<Waypoint> waypoints;
	
	public NavigationWaypointsPacket() {}
	
	public NavigationWaypointsPacket(List<Waypoint> waypoints) {
		this.waypoints = waypoints;
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
		}
	}

	@Override
	public void receive(JSONObject json) {
		int index = json.optInt("index");
		String action = json.optString("action");
		
		List<Waypoint> waypoints = EdisonControl.getInstance().getNavHandler().getWaypoints();
		if(index > waypoints.size() - 1) {
			logger.warn("Cannot edit waypoint (index {}) because it does not exist", index);
			return;
		}
		
		Waypoint waypoint = waypoints.get(index);
		
		if(action.equals("move")) {
			float x = json.optFloat("x");
			float y = json.optFloat("y");
			
			EdisonControl.getInstance().getNavHandler().moveWaypoint(waypoint, x, y);
		} else if(action.equals("remove")) {
			EdisonControl.getInstance().getNavHandler().removeWaypoint(waypoint);
		}
	}

	@Override
	public String getPacketName() {
		return PACKET_NAME;
	}

}
