package tech.mistermel.edisoncontrol.navigation;

import java.util.ArrayList;
import java.util.List;

public class CardinalSpline {

	private List<Location> controlPoints = new ArrayList<>();
	
	public Location calculatePoint(int index, float t) {
		Location p0 = controlPoints.get(index - 1);
		Location p1 = controlPoints.get(index);
		Location p2 = controlPoints.get(index + 1);
		Location p3 = controlPoints.get(index + 2);
	
		float t2 = t * t;
		float t3 = t * t * t;
		
		float q1 = -t3 + 2.0f * t2 - t;
		float q2 = 3.0f * t3 - 5.0f * t2 + 2.0f;
		float q3 = -3.0f * t3 + 4.0f * t2 + t;
		float q4 = t3 - t2;
		
		float tx = 0.5f * (p0.getX() * q1 + p1.getX() * q2 + p2.getX() * q3 + p3.getX() * q4);
		float ty = 0.5f * (p0.getY() * q1 + p1.getY() * q2 + p2.getY() * q3 + p3.getY() * q4);
		return new Location(tx, ty);
	}
	
	public List<Location> calculatePoints(int pointsPerSegment) {
		List<Location> locations = new ArrayList<>();
		float timeInterval = 1.0f / (float) pointsPerSegment;
		
		for(int index = 1; index < controlPoints.size() - 1; index++) {
			for(float time = 0; time <= 1.0f; time += timeInterval) {
				locations.add(this.calculatePoint(index, time));
			}
		}
		
		return locations;
	}
	
	public void importWaypoints(List<Waypoint> waypoints) {
		if(waypoints.size() < 3) {
			throw new IllegalArgumentException("Waypoints list must contain at least 3 waypoints");
		}
		
		controlPoints.clear();
		
		// The Catmull-Rom spline requires two control points that the line will not go through
		// in order to work. These two points are calculated here, based on the direction of the
		// first 2 and last 2 actual points.
		Location firstControlPoint = this.extrapolate(waypoints.get(0).getLocation(), waypoints.get(1).getLocation(), true);
		int n = waypoints.size() - 1;
		Location lastControlPoint = this.extrapolate(waypoints.get(n).getLocation(), waypoints.get(n - 1).getLocation(), false);
		
		controlPoints.add(firstControlPoint);
		for(Waypoint waypoint : waypoints) {
			controlPoints.add(waypoint.getLocation());
		}
		controlPoints.add(lastControlPoint);
	}
	
	private Location extrapolate(Location first, Location second, boolean backwards) {
		float dx = second.getX() - first.getX();
		float dy = second.getY() - first.getY();
		
		if(backwards) {
			return new Location(first.getX() - dx, first.getY() - dy);
		}
		
		return new Location(first.getX() + dx, first.getY() + dy);
	}
	
}
