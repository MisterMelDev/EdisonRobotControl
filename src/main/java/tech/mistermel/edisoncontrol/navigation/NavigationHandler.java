package tech.mistermel.edisoncontrol.navigation;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;
import tech.mistermel.edisoncontrol.SystemHealthHandler.HealthStatusType;
import tech.mistermel.edisoncontrol.SystemHealthHandler.Service;
import tech.mistermel.edisoncontrol.navigation.magnetometer.BNO055Interface;
import tech.mistermel.edisoncontrol.navigation.magnetometer.IMUProvider;
import tech.mistermel.edisoncontrol.navigation.route.CardinalSplineRoute;
import tech.mistermel.edisoncontrol.navigation.route.RouteProvider;
import tech.mistermel.edisoncontrol.web.packet.NavigationTelemetryPacket;
import tech.mistermel.edisoncontrol.web.packet.NavigationTogglePacket;
import tech.mistermel.edisoncontrol.web.packet.NavigationWaypointsPacket;
import tech.mistermel.edisoncontrol.web.packet.RoutePacket;

public class NavigationHandler {

	private static final Logger logger = LoggerFactory.getLogger(NavigationHandler.class);
	
	private static final int UPDATES_PER_SECOND = 10;
	private static final float DELTA_TIME = 1.0f / UPDATES_PER_SECOND;
	
	private static final float P = 0f;
	private static final float I = 0f;
	private static final float D = 0f;
	
	private IMUProvider imuProvider;
	private NavigationThread thread;
	private RouteProvider routeProvider;
	
	private boolean isActive = false;
	
	private List<Waypoint> waypoints = new ArrayList<>();
	private List<Location> splinePoints;
	private Location closestSplinePoint;
	
	private Location currentLoc = new Location();
	private float heading;
	private float[] acceleration;
	
	private float cte, cteOld;
	private float steeringFactor;
	private int steer, speed;
	
	public NavigationHandler() {
		JSONObject configSection = EdisonControl.getInstance().getConfigHandler().getJson().optJSONObject("navigation");
		if(configSection == null) {
			logger.warn("No 'navigation' configuration section present in config.json - this will cause errors!");
			return;
		}
		
		this.routeProvider = new CardinalSplineRoute(configSection.optInt("points_per_segment", 50));
		
		this.imuProvider = new IMUProvider(new BNO055Interface());
		imuProvider.start();
		
		this.thread = new NavigationThread();
		thread.start();
	}
	
	private class NavigationThread extends Thread {
		
		public NavigationThread() {
			super("NavigationThread");
		}
		
		@Override
		public void run() {		
			int millisDelay = 1000 / UPDATES_PER_SECOND;
			while(true) {
				long startTime = System.currentTimeMillis();
				
				NavigationTelemetryPacket packet = new NavigationTelemetryPacket(currentLoc, (int) heading, splinePoints == null ? -1 : splinePoints.indexOf(closestSplinePoint), acceleration, cte, steeringFactor, steer, speed);
				EdisonControl.getInstance().getWebHandler().sendPacket(packet);
				
				if(isActive) {
					tick();
				}
				
				try {
					long timeLeft = (startTime + millisDelay) - System.currentTimeMillis();
					if(timeLeft < 0) {
						logger.warn("Navigation handler cannot keep up! This tick took {}ms, absolute maximum should be {}ms", (System.currentTimeMillis() - startTime), millisDelay);
						timeLeft = 0;
					}
					
					Thread.sleep(timeLeft);
				} catch (InterruptedException e) {
					logger.error("Interrupted in navigation loop", e);
					Thread.currentThread().interrupt();
				}
			}
		}
		
		private void tick() {
			if(EdisonControl.getInstance().getSystemHealthHandler().getStatus(Service.SERIAL_DWM) != HealthStatusType.RUNNING) {
				logger.warn("DWM serial is not working! Exiting navigation mode.");
				setActive(false);
				EdisonControl.setStatus(Service.NAVIGATION, HealthStatusType.FAULT, "DWM serial not working");
				return;
			}
			
			closestSplinePoint = getClosestSplinePoint();
			cte = (float) closestSplinePoint.distanceTo(currentLoc);
			
			float alpha = cte * P;
			
			float dtCte = (cte - cteOld) / DELTA_TIME;
			alpha += D * dtCte;
			cteOld = cte;
			
			steeringFactor = alpha;
			setControls(0, (int) alpha);
		}
		
		private Location getClosestSplinePoint() {
			if(splinePoints == null) {
				return null;
			}
			
			Location nearestPoint = null;
			double nearestDistance = Double.MAX_VALUE;
			
			for(Location point : splinePoints) {
				double distance = point.distanceTo(currentLoc);
				if(distance < nearestDistance) {
					nearestPoint = point;
					nearestDistance = distance;
				}
			}
			
			return nearestPoint;
		}
		
		private double getHeadingDistance(float a, float b) {
			double left = a - b;
			double right = b - a;
			if(left < 0) left += 360;
			if(right < 0) right += 360;
			return left < right ? -left : right;
		}
		
	}
	
	private void initialize() {
		// Nothing implemented yet
	}
	
	private void stop() {
		this.closestSplinePoint = null;
		this.cte = 0;
	}
	
	private void updateRoute() {
		if(waypoints.size() >= 2) {
			routeProvider.importWaypoints(waypoints);
			this.splinePoints = routeProvider.calculatePoints();
		} else {
			this.splinePoints = null;
		}
		
		RoutePacket routePacket = new RoutePacket(splinePoints);
		EdisonControl.getInstance().getWebHandler().sendPacket(routePacket);
	}
	
	public void onPositionReceived(float x, float y) {
		currentLoc.setX(x);
		currentLoc.setY(y);
	}
	
	public void onHeadingReceived(float heading) {
		this.heading = heading;
	}
	
	public void onAccelerationReceived(float[] acceleration) {
		this.acceleration = acceleration;
	}
	
	public Waypoint createWaypoint(float x, float y) {
		logger.debug("Creating waypoint at x: {}, y: {}", x, y);
		
		Waypoint waypoint = new Waypoint(x, y);
		waypoints.add(waypoint);
		
		this.updateRoute();	
		this.sendWaypoints();
		return waypoint;
	}
	
	public void moveWaypoint(Waypoint waypoint, float x, float y) {
		logger.debug("Moving waypoint #{} to x: {}, y {}", waypoints.indexOf(waypoint), x, y);
		
		waypoint.setX(x);
		waypoint.setY(y);
		
		this.updateRoute();
		this.sendWaypoints();
	}
	
	public void removeWaypoint(Waypoint waypoint) {
		logger.debug("Removing waypoints #{}}", waypoints.indexOf(waypoint));
		
		waypoints.remove(waypoint);
		this.updateRoute();
		this.sendWaypoints();
	}
	
	public void sendWaypoints() {
		NavigationWaypointsPacket packet = new NavigationWaypointsPacket(waypoints);
		EdisonControl.getInstance().getWebHandler().sendPacket(packet);
	}
	
	public void clearWaypoints() {
		waypoints.clear();
	}
	
	public List<Waypoint> getWaypoints() {
		return waypoints;
	}
	
	public boolean setActive(boolean isActive) {
		if(isActive && waypoints.isEmpty()) {
			logger.warn("Cannot start navigation handler, no waypoints set");
			return false;
		}
		
		if(isActive && (splinePoints == null || splinePoints.isEmpty())) {
			logger.warn("Cannot start navigation handler, spline points not calculated");
			return false;
		}
		
		logger.info("{} navigation handler", isActive ? "Starting" : "Stopping");
		
		if(isActive) {
			this.initialize();
		} else {
			this.stop();
		}
		
		this.isActive = isActive;
		this.setControls(0, 0, true);
		
		NavigationTogglePacket packet = new NavigationTogglePacket(isActive);
		EdisonControl.getInstance().getWebHandler().sendPacket(packet);
		
		EdisonControl.setStatus(Service.NAVIGATION, isActive ? HealthStatusType.RUNNING : HealthStatusType.DISABLED);
		return true;
	}
	
	public void setControls(int speed, int steer) {
		this.setControls(speed, steer, false);
	}
	
	public void setControls(int speed, int steer, boolean force) {
		if(!isActive && !force) {
			logger.debug("Ignoring setControls() call with params {}, {} because nav handler is disabled and not forced", speed, steer);
			return;
		}
		
		this.steer = steer;
		this.speed = speed;
		
		EdisonControl.getInstance().getSerialInterface().setControls(speed, steer);
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public IMUProvider getIMUProvider() {
		return imuProvider;
	}
	
}
