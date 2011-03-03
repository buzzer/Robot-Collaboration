package device;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import data.Position;
import javaclient3.structures.PlayerPose;
import javaclient3.structures.planner.PlayerPlannerData;
import javaclient3.PlannerInterface;

public class Planner extends RobotDevice
{
	/** Logging support */
	Logger logger = Logger.getLogger (Planner.class.getName ());

	/** Callback listeners */
	CopyOnWriteArrayList<IPlannerListener> isDoneListeners;

	Position goal = null;
	Position globalGoal = null;

	PlayerPlannerData ppd = null;
	boolean isNewGoal = false;
	boolean notify = false;
	Position curPosition;
	boolean isDone;
	boolean isValidGoal;
	boolean isStopped;
	int wayPointCount;
	int wayPointIndex;

	/** Watchdog timer for goal checking */
	boolean timerIsArmed = false;
	Timer timer;

	public Planner(DeviceNode roboClient, Device device)
	{
		super(roboClient, device);

		goal = new Position();
		globalGoal = new Position();
		curPosition = new Position();
		isDoneListeners = new CopyOnWriteArrayList<IPlannerListener>();
		timer = new Timer();

		setSleepTime(500);

		/** Disable motion */
		((PlannerInterface) getDevice()).setRobotMotion(0);

		/** Let the planner still to refresh its internal data */
		isStopped = false;
	}
	protected void update ()
	{
//		if(isStopped == true) return;

		/** Check for ready planner data */
		if (((PlannerInterface) getDevice()).isDataReady()) {

			/** request recent planner data */
			ppd = ((PlannerInterface) getDevice()).getData ();

			/** Check for a valid path */
			if (ppd.getValid() == new Integer(1).byteValue())
				isValidGoal = true;
			else
				isValidGoal = false;

			/** Check if goal is achieved */
			if (ppd.getDone() == new Integer(1).byteValue())
			{
				/** Planner is done */
				// Check if really at the goal position
				if (globalGoal.distanceTo(curPosition) < 1.0) {
					isDone = true;
					stopTimer();
					notifyListeners();
				} else {
					/** Set the goal again */
					setGoal(globalGoal);
				}
			}
			else /** Planner is not yet done */
			{
				if (isValidGoal != true) {
					isDone = true;
				} else {
					isDone = false;					
					/** Check if planner is maybe stucked */
					if (goal.distanceTo(curPosition) < 1.0)	{
						startTimer();
					} else {
						stopTimer();
					}
				}
			}

			wayPointCount = ppd.getWaypoints_count();
			wayPointIndex = ppd.getWaypoint_idx();

			PlayerPose poseTemp = ppd.getPos();
			/** Update current position belief */
			if (poseTemp != null) {
				curPosition.setX(poseTemp.getPx());
				curPosition.setY(poseTemp.getPy());
				curPosition.setYaw(poseTemp.getPa());
			}

			/** Set new goal */
			if(isNewGoal)
			{
				isNewGoal = false;
				((PlannerInterface) getDevice()).setGoal(new PlayerPose(
						goal.getX(),
						goal.getY(),
						goal.getYaw()));
			}
			else /** Get current goal */
			{
				if (((PlannerInterface) getDevice()).isReadyWaypointData() == true) {
					poseTemp = ((PlannerInterface) getDevice()).getData().getGoal();
					if (poseTemp != null) {
						goal.setX(poseTemp.getPx());
						goal.setY(poseTemp.getPy());
						goal.setYaw(poseTemp.getPa());
					}
				}
			}
			logger.fine(
					"WPCnt: "+this.wayPointCount
					+" WPIdx: "+this.wayPointIndex
					+" CurGoal: "+this.goal.toString()
					+" CurPos: "+this.curPosition.toString()
					+" IsDone: "+this.isDone
					+" IsValid: "+this.isValidGoal);
		}
	}
	/**
	 * Sets the planner goal to the given @see Position.
	 * @param newGoal Position to navigate to.
	 * @return �true� if the planner could plan a trajectory, �false' else.
	 */
	public synchronized boolean setGoal (Position newGoal) {
		// New Positions and copy
		goal = new Position(newGoal);
		globalGoal = new Position(newGoal);
		isNewGoal = true;
		isValidGoal = false;
		isDone = false;
		isStopped = false;
		// enable motion
		((PlannerInterface) getDevice()).setRobotMotion(1);

		try { Thread.sleep(getSleepTime()*2); } catch (InterruptedException e) { /* e.printStackTrace();*/ }

		notify = true; // notify listeners of this new goal

		return isValidGoal();
	}
	public Position getGoal() {
		return new Position(globalGoal);
	}
	/**
	 * @depreciated Use @Localize#setPosition instead.
	 * @param position
	 */
	public synchronized void setPosition(Position position) {
		//		curPosition.setPosition(position);
		//		isNewPose = true;
	}
	public synchronized Position getPosition() {
		return new Position(curPosition);
	}
	public boolean isDone() {
		return isDone;
	}
	public boolean isValidGoal() {
		return isValidGoal;
	}
	public int getWayPointCount() {
		return wayPointCount;
	}
	public int getWayPointIndex() {
		return wayPointIndex;
	}
	/**
	 * Stops approaching the current goal (if any).
	 * Does not remove goal from goal stack but stops robot motion.
	 */
	public boolean stop() {
		isStopped  = true;
		
		// disable motion
		((PlannerInterface) getDevice()).setRobotMotion(0);

		return true;
	}
	/**
	 * Returns the current cost to current goal position.
	 * @return The cost value.
	 */
	public double getCost() {
		try { Thread.sleep(getSleepTime()); } catch (InterruptedException e) { /* e.printStackTrace();*/ }

		if (isActive() == false) {
			return -1.0;
		} else {
			logger.info("wayPointCount: "+wayPointCount+" wayPointIndex: "+wayPointIndex+" distance: "+globalGoal.distanceTo(curPosition));
			return (1 + wayPointCount - wayPointIndex) * globalGoal.distanceTo(curPosition);
		}
	}
	/**
	 * Returns the cost if the goal would be the given position.
	 * The planner does not actually approach the given goal but
	 * resumes the previous set one (if any).
	 * @param toPosition @see Position to calculate the cost.
	 * @return The cost value.
	 */
	public synchronized double getCost(Position toPosition) {
		Position oldGoal = null;
		double cost = -1.0;

		if (isActive() == true) {
			// Suspend current goal
			oldGoal = globalGoal;
			stop();
		}

		if (setGoal(toPosition) == true)
			cost = getCost();

		stop();

		// Resume old goal if any
		if (oldGoal != null)
			setGoal(oldGoal);
		else
			stop();

		return cost;
	}
	public Logger getLogger() {
		return logger;
	}
	public void addIsDoneListener(IPlannerListener cb){
		isDoneListeners.addIfAbsent(cb);
	}
	public void removeIsDoneListener(IPlannerListener cb){
		isDoneListeners.remove(cb);
	}
	public void notifyListeners() {
		if (notify == true) {
			notify = false;
			Iterator<IPlannerListener> it = isDoneListeners.iterator();
			while (it.hasNext()) { it.next().callWhenIsDone(); }
		}
	}
	@Override synchronized public void shutdown() {
		super.shutdown();
		isDoneListeners.clear();
		stopTimer();
	}
	/**
	 * Checks if the planner is currently busy with a plan.
	 * @return 'true' if planner is busy, 'false' else.
	 */
	public boolean isActive() {
		if (isDone() == true)
			return false;
		else
			if (isValidGoal() == false)
				return false;

		return true;
	}
	/**
	 * Start a timer if not already a timer is running.
	 */
	synchronized void startTimer() {
		if ( timerIsArmed == false) {
			timerIsArmed = true;
			logger.info("Set up planner watchdog timer");

			timer.schedule(new TimerTask()
			{
				public void run()
				{
					timerIsArmed = false;
					logger.info("Watchdog occured");
					// Check if not already finished
					if (isDone() == false) {
						notifyListeners(); // TODO debug setDone();
						stop();

						logger.config("Timout for goal: "+globalGoal.toString());
					}
				}
			}, 20000);
		}
	}
	/**
	 * Stop a timer if already a timer has been started.
	 */
	synchronized void stopTimer() {
		if (timerIsArmed == true) {
			timerIsArmed = false;
			timer.cancel();
			timer = new Timer();

			logger.info("Watchdog canceled");
		}
	}
}
