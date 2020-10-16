package tech.mistermel.edisoncontrol.navigation.route;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.navigation.Location;
import tech.mistermel.edisoncontrol.navigation.Waypoint;

public class CardinalSplineRoute implements RouteProvider {

	private static final Logger logger = LoggerFactory.getLogger(CardinalSplineRoute.class);
	
	private List<Location> controlPoints = new ArrayList<>();
	private int pointsPerSegment;
	
	public CardinalSplineRoute(int pointsPerSegment) {
		this.pointsPerSegment = pointsPerSegment;
	}
	
	public Location calculatePoint(int index, float t) {
		Location p0 = controlPoints.get(index - 1);
		Location p1 = controlPoints.get(index);
		Location p2 = controlPoints.get(index + 1);
		Location p3 = controlPoints.get(index + 2);
	
		float alpha = 1f;
		
		float t2 = t * t;
		float t3 = t * t * t;
		
		float a = 2 * t3 - 3 * t2 + 1;
		float b = -2 * t3 + 3 * t2;
		float c = alpha * (t3 - 2 * t2 + t);
		float d = alpha * (t3 - t2);
		
		float tx = a * p1.getX() + b * p2.getX() + c * (p2.getX() - p0.getX()) + d * (p3.getX() - p1.getX());
		float ty = a * p1.getY() + b * p2.getY() + c * (p2.getY() - p0.getY()) + d * (p3.getY() - p1.getY());
		return new Location(tx, ty);
	}
	
	@Override
	public List<Location> calculatePoints() {
		List<Location> locations = new ArrayList<>();
		float timeInterval = 1.0f / (float) pointsPerSegment;
		
		for(int index = 1; index < controlPoints.size() - 2; index++) {
			for(float time = 0; time <= 1.0f; time += timeInterval) {
				locations.add(this.calculatePoint(index, time));
			}
		}
		
		return locations;
	}
	
	@Override
	public void importWaypoints(List<Waypoint> waypoints) {
		if(waypoints.size() < 2) {
			throw new IllegalArgumentException("Waypoints list must contain at least 2 waypoints");
		}
		
		controlPoints.clear();
		
		// The Catmull-Rom spline requires two control points that the line will not go through
		// in order to work. These two points are calculated here, based on the direction of the
		// first 2 and last 2 actual points.
		Location firstControlPoint = this.extrapolate(waypoints.get(0), waypoints.get(1), true);
		int n = waypoints.size() - 1;
		Location lastControlPoint = this.extrapolate(waypoints.get(n), waypoints.get(n - 1), false);
		
		controlPoints.add(firstControlPoint);
		for(Waypoint waypoint : waypoints) {
			controlPoints.add(waypoint);
		}
		controlPoints.add(lastControlPoint);
		
		logger.debug("Imported waypoints into cardinal spline ({} waypoints, {} control points)", waypoints.size(), controlPoints.size());
	}
	
	private Location extrapolate(Location first, Location second, boolean backwards) {
		float dx = second.getX() - first.getX();
		float dy = second.getY() - first.getY();
		return new Location(first.getX() + (backwards ? -dx : dx), first.getY() + (backwards ? -dy : dy));
	}
	
}
