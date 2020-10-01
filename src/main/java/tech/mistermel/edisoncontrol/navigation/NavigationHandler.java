package tech.mistermel.edisoncontrol.navigation;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.mistermel.edisoncontrol.EdisonControl;

public class NavigationHandler extends Thread {
	
	public static void main(String[] args) {
		new NavigationHandler().setActive(true);
	}

	private static final Logger logger = LoggerFactory.getLogger(NavigationHandler.class);
	private static final int UPDATES_PER_SECOND = 10;
	private static final int ROTATE_IN_PLACE_TRESHOLD = 45;
	private static final int SPEED = 200;
	
	private boolean isActive = false;
	private List<Waypoint> waypoints = new ArrayList<>();
	
	private float x, y;
	private float heading;
	
	private Waypoint target;
	private float targetHeading;
	
	public NavigationHandler() {
		super("NavigationThread");
	}
	
	@Override
	public void run() {
		int millisDelay = 1000 / UPDATES_PER_SECOND;
		
		while(isActive) {
			long startTime = System.currentTimeMillis();
			this.tick();
			
			try {
				long timeLeft = (startTime + millisDelay) - System.currentTimeMillis();
				if(timeLeft < 0) {
					logger.warn("Navigation handler cannot keep up! This tick took {}ms, absolute maximum should be {}ms", (System.currentTimeMillis() - startTime), millisDelay);
					timeLeft = 0;
				}
				
				Thread.sleep(timeLeft);
			} catch (InterruptedException e) {}
		}
		
		logger.debug("Navigation handler loop exited");
		this.stopNavigation();
	}
	
	private void tick() {
		float headingDistance = heading - targetHeading;
		int steer = (int) headingDistance * 5;
		
		if(Math.abs(headingDistance) >= ROTATE_IN_PLACE_TRESHOLD) {
			this.setControls(0, steer);
			return;
		}
		
		this.setControls(SPEED, steer);
	}
	
	public void setTargetedWaypoint(Waypoint waypoint) {
		int waypointIndex = waypoints.indexOf(waypoint);
		if(waypointIndex == -1) {
			logger.warn("Cannot target waypoint, is not in waypoint list");
			return;
		}
		
		logger.info("New waypoint target (index: {}, x: {}, y: {})", waypointIndex, target.getX(), target.getY());
		this.target = waypoint;
	}
	
	public void onPositionReceived(float x, float y) {
		this.x = x;
		this.y = y;
		
		if(target != null) {
			this.targetHeading = calculateHeading(x, y, target.getX(), target.getY());
		}
		
		EdisonControl.getInstance().getWebHandler().sendPosition(x, y, heading);
	}
	
	public void onHeadingReceived(float heading) {
		this.heading = heading;
	}
	
	private float calculateHeading(float originX, float originY, float targetX, float targetY) {
		double radians = Math.atan2(targetY - originY, targetX - originX);
		return (float) Math.toDegrees(radians);
	}
	
	public Waypoint createWaypoint(float x, float y) {
		Waypoint waypoint = new Waypoint(x, y);
		waypoints.add(waypoint);
		return waypoint;
	}
	
	public List<Waypoint> getWaypoints() {
		return waypoints;
	}
	
	public boolean setActive(boolean isActive) {
		return isActive ? startNavigation() : stopNavigation();
	}
	
	private boolean startNavigation() {
		logger.info("Starting navigation handler");
		this.isActive = true;
		this.setControls(0, 0, true);
		
		this.start();
		return true;
	}
	
	private boolean stopNavigation() {
		logger.info("Stopping navigation handler");
		this.isActive = false;
		this.setControls(0, 0, true);
		
		try {
			this.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
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
