package data;

import java.util.Date;

import robot.external.IRobot;

import device.Simulation;

// Blackboard note object
public class BbNote {

	protected Simulation simu = null;
	protected String key = "";
	protected Position pose = null;
	protected Position oldPose = null;

	protected Position goal = null;
	protected boolean isGoal = false;
	protected IRobot tracked = null;
	protected boolean completed = false;
	protected long lastChecked = 0;
	protected int timeout = 2000;
	protected double epsilon = 0.5; // meters
	
	public boolean isCompleted() {
		return completed;
	}

	public BbNote() {
//		lastChecked = new Date().getTime();
		// TODO test values
//		goal2 = new Position(-7,-7,0);
	}
	
	public void setPose( Position pose2 ) {
		this.pose = pose2;
	}
	public Position getPose() {
		return this.pose;
	}
	public Position getGoal() {
		return goal;
	}

	public void setGoal(Position goal) {
		this.goal = goal;
	}

	public void setTrackable (IRobot tracked2) {
		this.tracked = tracked2;
	}
	public IRobot getTrackable () {
		return this.tracked;
	}

	public void update() {
		// if there is a position, goal and something to bring you from,there..
		if (pose != null && goal != null && tracked != null) {
			// check if robot is already there
			boolean goalReached = goalReached();
			if ( goalReached ) {
				this.completed = true;
				// Set object in simulator
				if (simu != null && key != "") {
					simu.setPositionOf(key, new Position(-3, -5, 0));
					System.out.println("Setting " + key + " back");
				}
			} else {
				if ( timeout() == true ) {
					System.out.println("Current goal: " + tracked.getGoal().toString());
//					if ( tracked.getGoal().isEqualTo(goal)) {
					// Setting goal again
					tracked.setGoal(goal);
					System.out.println("Setting new goal: " + goal.toString());
					//				if( ! tracked.getGoal().isEqualTo(goal1)){
					//					tracked.setGoal(goal1);
//					}
				} else {
					// Do nothing
//					System.out.println("Goal is being processed: " + tracked.getGoal().toString());
				}
			}
		}
	}

	private boolean timeout() {
		boolean isTimeout = false;
		
		// get current time
		long now = new Date().getTime();

		// 1st time then timeout
		if (lastChecked == 0) {
			lastChecked = now;
			oldPose = tracked.getPosition();
			isTimeout = true;
		} else {
			if ( (lastChecked + timeout) <= now) {
				lastChecked = now;
				// timeout
				// Check for pose change
				// get current position
				Position curPos = tracked.getPosition();
//				System.err.println("current robot pose updated: " + curPos.toString());

				if (curPos.distanceTo(oldPose) < 0.1) {
					// no progress done: timeout
					System.err.println("Timeout: Robot has not moved.");
					System.err.println("current robot pose updated: " + curPos.toString());
					System.err.println("old robot pose updated: " + oldPose.toString());

					isTimeout = true;
				}
				oldPose = curPos;
			}
		}
		return isTimeout;
	}

	private boolean goalReached() {
		if ( pose != null && goal != null) {
			// get last robot position
			Position robotpos = tracked.getPosition();
			// Euclidean distance, Pythagoras
//			if (distance(robotpos, goal) < epsilon) {
			if (robotpos.equals(goal) == true) {
				System.out.println("Goal reached");
				return true;
			}
		}
		//		System.out.println("Goal not reached yet");
		return false;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setSimu(Simulation simu) {
		this.simu = simu;
	}	
}
