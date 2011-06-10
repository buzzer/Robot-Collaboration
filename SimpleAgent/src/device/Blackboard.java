package device;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;

import robot.external.IRobot;
import data.BbNote;
import data.Position;

/**
 * Blackboard implementation.
 * @deprecated Use {@link data.Board} instead.
 * @author sebastian
 *
 */
public class Blackboard extends Device {
	
	protected static Blackboard instance = null;
	protected static Simulation simu = null;
	protected IRobot collectrobot = null;

//	protected LinkedHashMap<String,BbNote> notehm = null;
	protected ConcurrentHashMap<String,BbNote> notehm = null;
	
	Blackboard()
	{
		notehm = new ConcurrentHashMap<String,BbNote>();
		setSleepTime(500);		
	}
	
	Blackboard(IRobot robot)
	{
		this();
		try {
			collectrobot = robot;
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new IllegalStateException();
		}
	}
	public static Blackboard getInstance (IRobot robot)
	{
		if (instance == null) {
			instance = new Blackboard(robot);
		}
		return instance;
		
	}
	public static Blackboard getInstance () 
	{
		if (instance == null) {
			instance = new Blackboard();
		}
		return instance;
	}
	@SuppressWarnings("rawtypes")
	@Override
	public void update () {
		Set set = notehm.entrySet();
		Iterator i = set.iterator();
		// Track the 1st only
		// FIFO HashMap to keep goal order
		if(i.hasNext()) {
			Map.Entry me = (Map.Entry)i.next();
			String key = (String)me.getKey();
			BbNote note = (BbNote)me.getValue();
			if (note.isCompleted()) {
				notehm.remove(key);
				System.out.println("Removed note from BB: " + key); 
			} else {
				note.update();
//				System.out.println("Update of note : " + key);
			}
		} else {
			// check if already there
			if (collectrobot.getPosition() != null) {
				if (collectrobot.getPosition().distanceTo(new Position(-3,-5,0)) < 1) {
					Position robotPose = collectrobot.getPosition();
					if (robotPose != null) {
						if ( robotPose.distanceTo(new Position(-3,-5,0)) > 1 ) {
							// Always have gohome target
							BbNote gohome = new BbNote();
							gohome.setPose(robotPose);
							gohome.setGoal(new Position(-3,-5,0));
							gohome.setTrackable(collectrobot);
							notehm.put("gohome", gohome);
							System.out.println("Added gohome target");
						}
					}
				}
			}
		}		
	}
	
	public void add(String key, BbNote note) {
		if ( notehm.containsKey(key) == false ) {
			// TODO for testing only
			note.setTrackable(collectrobot);
			note.setKey(key);
			note.setSimu(simu);
			notehm.put(key, note);
			System.out.println("BB: added note " + key);
			
		}
		// remove idle task
		if (notehm.containsKey("gohome")) {
			// remove to be inserted at the end of FIFO
			notehm.remove("gohome");
		}
//		System.out.println("Added note: " + key + "\t" + note.getPose() + "\t" + note.getGoal());
	}
	public BbNote get(String key) {
		return this.notehm.get(key);
	}
	public void setSimulation (Simulation simu2) {
		simu = simu2;
	}
	@SuppressWarnings("rawtypes")
	@Override
	public void shutdown() {
		// TODO debug only
		Set set = this.notehm.entrySet();
		Iterator i = set.iterator();

		while (i.hasNext()) {
			Map.Entry me = (Map.Entry)i.next();
			String key = (String)me.getKey();
			System.out.println("Still on blackboard: " + key);
		}
		super.shutdown();
	}
}
