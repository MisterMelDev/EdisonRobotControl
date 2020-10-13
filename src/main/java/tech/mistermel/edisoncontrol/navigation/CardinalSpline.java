package tech.mistermel.edisoncontrol.navigation;

import java.util.ArrayList;
import java.util.List;

public class CardinalSpline {

	private List<Location> controlPoints = new ArrayList<>();
	
	public Location calculatePoint(float globalTime) {
		int index = (int) globalTime;
		float t = globalTime - index;
		
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
	
}
