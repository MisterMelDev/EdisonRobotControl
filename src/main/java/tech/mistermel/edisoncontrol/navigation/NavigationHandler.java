package tech.mistermel.edisoncontrol.navigation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;

public class NavigationHandler {

	private static final Logger logger = LoggerFactory.getLogger(NavigationHandler.class);
	private static final int UPDATES_PER_SECOND = 10;
	private static final int ROTATE_IN_PLACE_TRESHOLD = 45;
	private static final int SPEED = 200;
	
	private MagnetometerProvider magnetometerProvider;
	private NavigationThread thread;
	
	private boolean isActive = false;
	private List<Waypoint> waypoints = new ArrayList<>();
	
	private float x, y;
	private float heading;
	
	private Waypoint target;
	private float targetHeading;
	
	public NavigationHandler() {
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
				
				EdisonControl.getInstance().getWebHandler().sendPosition(x, y, (int) heading, isActive ? (int) targetHeading : 0);
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
				return;
			}
			
			float headingDistance = heading - targetHeading;
			int steer = (int) headingDistance * 5;
			
			if(Math.abs(headingDistance) >= ROTATE_IN_PLACE_TRESHOLD) {
				setControls(0, steer);
				return;
			}
			
			setControls(SPEED, steer);
		}
	}
	
	public void setTargetedWaypoint(Waypoint waypoint) {
		int waypointIndex = waypoints.indexOf(waypoint);
		if(waypointIndex == -1) {
			logger.warn("Cannot target waypoint, is not in waypoint list");
			return;
		}
		
		this.target = waypoint;
		logger.info("New waypoint target (index: {}, x: {}, y: {})", waypointIndex, target.getX(), target.getY());
		
		this.sendWaypoints();
	}
	
	public void onPositionReceived(float x, float y) {
		this.x = x;
		this.y = y;
		
		if(target != null) {
			this.targetHeading = calculateHeading(x, y, target.getX(), target.getY());
		}
	}
	
	public void onHeadingReceived(float heading) {
		this.heading = heading;
	}
	
	private float calculateHeading(float originX, float originY, float targetX, float targetY) {
		double radians = Math.atan2(targetY - originY, targetX - originX);
		return (float) Math.toDegrees(radians);
	}
	
	public Waypoint createWaypoint(float x, float y) {
		logger.info("Creating waypoint at x: {}, y: {}", x, y);
		
		Waypoint waypoint = new Waypoint(x, y);
		waypoints.add(waypoint);
		
		this.sendWaypoints();
		return waypoint;
	}
	
	public void sendWaypoints() {
		EdisonControl.getInstance().getWebHandler().sendWaypoints(waypoints, target);
	}
	
	public void clearWaypoints() {
		waypoints.clear();
		target = null;
	}
	
	public List<Waypoint> getWaypoints() {
		return waypoints;
	}
	
	public boolean setActive(boolean isActive) {
		if(isActive && waypoints.size() == 0) {
			logger.warn("Cannot start navigation handler, no waypoints set");
			return false;
		}
		
		logger.info("{} navigation handler", isActive ? "Starting" : "Stopping");
		this.isActive = isActive;
		this.setControls(0, 0, true);
		
		if(isActive) {
			this.setTargetedWaypoint(waypoints.get(0));
		}
		
		EdisonControl.getInstance().getWebHandler().updateNavigationState();
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
		
		EdisonControl.getInstance().getSerialInterface().setControls(speed, steer);
	}
	
	public boolean isActive() {
		return isActive;
	}
	
}
