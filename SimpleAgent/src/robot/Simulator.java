package robot;

import java.util.*;

import javaclient3.PlayerClient;
import javaclient3.PlayerException;
import javaclient3.SimulationInterface;
import javaclient3.structures.PlayerConstants;
import javaclient3.structures.PlayerPose;
import javaclient3.structures.simulation.PlayerSimulationPose2dReq;

public class Simulator implements Runnable {
	
	protected PlayerClient playerclient = null;
	protected SimulationInterface simu = null;
	protected static Simulator instance = null;
	
	protected HashMap<String,Position> objList = null;

	// Every class of this type has it's own thread
	protected Thread thread = new Thread ( this );

	// Singleton
	protected Simulator(String name, int port) {
		try {
			// Connect to the Player server and request access to Position
			this.playerclient  = new PlayerClient (name, port);
			System.out.println("Running playerclient of: "
					+ this.toString()
					+ " in thread: "
					+ this.playerclient.getName());
			
			this.simu = this.playerclient.requestInterfaceSimulation(0, PlayerConstants.PLAYER_OPEN_MODE);
			this.objList = new HashMap<String, Position>();
			
			// Automatically start own thread in constructor
			this.thread.start();
			
			System.out.println("Running "
					+ this.toString()
					+ " in thread: "
					+ this.thread.getName());
			
		} catch (PlayerException e) {
			System.err.println ("Simulator: > Error connecting to Player: ");
			System.err.println ("    [ " + e.toString() + " ]");
			System.exit (1);
		}
	}
	
	public static Simulator getInstance (String name, int port) {
		if (instance == null) {
			instance = new Simulator(name, port);
		}
		return instance;
		
	}
	// TODO Currently only 'static' objects should be modified
	@SuppressWarnings("rawtypes")
	protected void update () {
//		while ( ! this.simu.isDataReady() ) { // TODO debug it
//		PlayerPose pp = new PlayerPose(7,7,0);
//		this.simu.set2DPose(identifier, pp);
		Set set = this.objList.entrySet();
		Iterator i = set.iterator();
		while(i.hasNext()) {
			Map.Entry me = (Map.Entry)i.next();
			String key = (String)me.getKey();
			Position pos = (Position)me.getValue();
			PlayerPose pp = new PlayerPose(pos.x, pos.y, pos.yaw);
			this.simu.set2DPose(key, pp);
		}
		try { Thread.sleep (200); }
		catch (InterruptedException e) { this.thread.interrupt(); }
//		}
	}

	@Override
	public void run() {
//		this.test();
		while ( ! this.thread.isInterrupted()) {
			// Should not be called more than @ 10Hz
			this.update();
		}
	}
	// Shutdown simulator and clean up
	public void shutdown () {
		// Cleaning up
		this.playerclient.close();
		this.thread.interrupt();
		while(this.thread.isAlive());
		System.out.println("Shutdown of " + this.toString());
	}
	public void setObject(String key, Position value) {
		objList.put(key, value);
	}
	protected void test () {
		this.simu.get2DPose("r3");
		if ( simu.isPose2DReady() ) {
			PlayerSimulationPose2dReq pose = simu.getSimulationPose2D ();
			System.out.printf ("Pose of %s: (%5.2f,%5.2f,%5.2f)\n",
					pose.getPose().getPx(),
					pose.getPose().getPy(),
					pose.getPose().getPa());
		}
		PlayerPose pp = new PlayerPose(7,7,0);
		this.simu.set2DPose("r2", pp);
//		this.simu.get2DPose("r1");
	}
}