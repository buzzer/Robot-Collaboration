package test;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.junit.Test;

import data.Position;
import device.Device;
import device.DeviceNode;
import device.IDevice;
import device.Localize;
import device.Planner;
import device.Simulation;

public class PlannerTest extends TestCase {
	
	static Planner planner = null;
	static Localize localizer = null;
	static DeviceNode deviceNode = null;
	static Simulation simu = null;

	@Test
	public void testPlanner() {
		deviceNode = new DeviceNode("localhost", 6666);
		assertNotNull(deviceNode);
		deviceNode.getClient().getLogger().setLevel(Level.FINEST);
		
		DeviceNode deviceNode2 = new DeviceNode("localhost", 6665);
		assertNotNull(deviceNode2);
		
		deviceNode.addDevicesOf(deviceNode2);

		deviceNode.runThreaded();
		
		assertEquals(deviceNode.isRunning(), true);
		assertEquals(deviceNode.isThreaded(), true);
		
		planner = (Planner) deviceNode.getDevice(new Device(IDevice.DEVICE_PLANNER_CODE, null, -1, -1));
		localizer = (Localize) deviceNode.getDevice(new Device(IDevice.DEVICE_LOCALIZE_CODE, null, -1, -1));
		simu = (Simulation) deviceNode.getDevice(new Device(IDevice.DEVICE_SIMULATION_CODE, null, -1, -1));
		
		assertNotNull(planner);
		assertEquals(planner.getClass(),Planner.class);
		assertEquals(planner.isRunning(), true);
		assertEquals(planner.isThreaded(), true);
		
//		planner.cancel();
		planner.setDebug(true);
	}

	@Test
	public void testSetPosition() {
		Position pose = new Position(-6,-5,Math.toRadians(90));
		
		simu.setPositionOf("r0", pose);
		while(simu.getPositionOf("r0").isNearTo(pose) != true);
		
//		planner.setPosition(pose);
		localizer.setPosition(pose);
		
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		
		assertTrue(planner.getPosition().isNearTo(pose));
	}

	@Test
	public void testSetGoal() {

		Position pose = new Position(-6.5,-2,Math.toRadians(75));

		for (int i=0; i<3; i++) {
			
			planner.setGoal(pose);
			try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
//			assertTrue(planner.isValidGoal());
			assertTrue(planner.getGoal().equals(pose));
			
			try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }

			assertTrue(planner.isDone());
			
			pose.setX(pose.getX()+2);
			pose.setYaw(0);
		}
	}

	@Test
	public void testShutdown() {
		deviceNode.shutdown();
	}

}