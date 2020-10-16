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
import tech.mistermel.edisoncontrol.navigation.magnetometer.MagnetometerProvider;
import tech.mistermel.edisoncontrol.navigation.route.CardinalSplineRoute;
import tech.mistermel.edisoncontrol.navigation.route.RouteProvider;
import tech.mistermel.edisoncontrol.web.packet.NavigationTelemetryPacket;
import tech.mistermel.edisoncontrol.web.packet.NavigationTogglePacket;
import tech.mistermel.edisoncontrol.web.packet.NavigationWaypointsPacket;
import tech.mistermel.edisoncontrol.web.packet.RoutePacket;

public class NavigationHandler {

	private static final Logger logger = LoggerFactory.getLogger(NavigationHandler.class);
	private static final int UPDATES_PER_SECOND = 10;
	private static final int ROTATE_IN_PLACE_TRESHOLD = 45;
	private static final int SPEED = 200;
	
	private MagnetometerProvider magnetometerProvider;
	private NavigationThread thread;
	
	private RouteProvider routeProvider;
	private List<Location> splinePoints;
	
	private boolean isActive = false;
	private List<Waypoint> waypoints = new ArrayList<>();
	
	private Location currentLoc = new Location();
	private float heading;
	
	private Waypoint target;
	private float targetHeading, targetDistance;
	
	public NavigationHandler() {
		JSONObject configSection = EdisonControl.getInstance().getConfigHandler().getJson().optJSONObject("navigation");
		if(configSection == null) {
			logger.warn("No 'navigation' configuration section present in config.json - this will cause errors!");
			return;
		}
		
		this.routeProvider = new CardinalSplineRoute(configSection.optInt("points_per_segment", 50));
		
		this.magnetometerProvider = new MagnetometerProvider(new BNO055Interface());
		magnetometerProvider.start();
		
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
				
				NavigationTelemetryPacket packet = new NavigationTelemetryPacket(currentLoc.getX(), currentLoc.getY(), targetDistance, (int) heading, (int) targetHeading);
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
			if(!EdisonControl.getInstance().getDWMSerialInterface().isCommunicationWorking()) {
				logger.warn("DWM serial is not working! Exiting navigation mode.");
				setActive(false);
				EdisonControl.setStatus(Service.NAVIGATION, HealthStatusType.FAULT, "DWM serial not working");
				return;
			}
			
			if(targetDistance < 0.3) {
				logger.info("Waypoint reached");
				waypoints.remove(target);
				
				if(waypoints.isEmpty()) {
					target = null;
					targetHeading = 0;
					targetDistance = 0;
					
					sendWaypoints();
					setActive(false);
					
					logger.info("Navigation finished");
					return;
				}
				
				setTargetedWaypoint(waypoints.get(0));
			}
			
			double headingDistance = getHeadingDistance(heading, targetHeading);
			int steer = (int) headingDistance * 5;
			
			if(steer > SPEED) {
				steer = SPEED;
			} else if(steer < -SPEED) {
				steer = -SPEED;
			}
			
			if(Math.abs(headingDistance) >= ROTATE_IN_PLACE_TRESHOLD) {
				setControls(0, steer);
				return;
			}
			
			setControls(SPEED, steer);
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
	
	public void setTargetedWaypoint(Waypoint waypoint) {
		int waypointIndex = waypoints.indexOf(waypoint);
		if(waypointIndex == -1) {
			logger.warn("Cannot target waypoint, is not in waypoint list");
			return;
		}
		
		this.target = waypoint;
		
		Location targetLoc = target.getLocation();
		logger.info("New waypoint target (index: {}, x: {}, y: {})", waypointIndex, targetLoc.getX(), targetLoc.getY());
		
		this.sendWaypoints();
	}
	
	public void onPositionReceived(float x, float y) {
		currentLoc.setX(x);
		currentLoc.setY(y);
		
		if(target != null) {
			Location targetLoc = target.getLocation();
			this.targetHeading = calculateTargetHeading(x, y, targetLoc.getX(), targetLoc.getY());
			this.targetDistance = calculateDistance(x, y, targetLoc.getX(), targetLoc.getY());
		}
	}
	
	public void onHeadingReceived(float heading) {
		this.heading = heading;
	}
	
	private float calculateTargetHeading(float originX, float originY, float targetX, float targetY) {
		double radians = Math.atan2(targetY - originY, targetX - originX);
		return (float) Math.toDegrees(radians) + 90;
	}
	
	private float calculateDistance(float originX, float originY, float targetX, float targetY) {
		return (float) Math.sqrt(Math.pow(originX - targetX, 2) + Math.pow(originY - targetY, 2));
	}
	
	public Waypoint createWaypoint(float x, float y) {
		logger.debug("Creating waypoint at x: {}, y: {}", x, y);
		
		Waypoint waypoint = new Waypoint(x, y);
		waypoints.add(waypoint);
		
		this.updateRoute();
		
		if(waypoints.size() == 1) {
			this.setTargetedWaypoint(waypoint);
		}
		
		this.sendWaypoints();
		return waypoint;
	}
	
	public void moveWaypoint(Waypoint waypoint, float x, float y) {
		logger.debug("Moving waypoint #{} to x: {}, y {}", waypoints.indexOf(waypoint), x, y);
		
		Location loc = waypoint.getLocation();
		loc.setX(x);
		loc.setY(y);
		
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
		NavigationWaypointsPacket packet = new NavigationWaypointsPacket(waypoints, target);
		EdisonControl.getInstance().getWebHandler().sendPacket(packet);
	}
	
	public void clearWaypoints() {
		waypoints.clear();
		target = null;
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
		
		logger.debug("Controlling! {} {}", speed, steer);
		EdisonControl.getInstance().getSerialInterface().setControls(speed, steer);
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public MagnetometerProvider getMagnetometerProvider() {
		return magnetometerProvider;
	}
	
}
