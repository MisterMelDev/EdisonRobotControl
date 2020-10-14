package tech.mistermel.edisoncontrol.navigation.route;

import java.util.List;

import tech.mistermel.edisoncontrol.navigation.Location;
import tech.mistermel.edisoncontrol.navigation.Waypoint;

public interface RouteProvider {

	public List<Location> calculatePoints(int pointsPerSegment);
	public void importWaypoints(List<Waypoint> waypoints);
	
}
