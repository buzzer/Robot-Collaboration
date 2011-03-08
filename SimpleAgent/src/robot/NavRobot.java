package robot;

import device.Device;

/**
 * A navigation robot.
 * It can be given goal positions and it will plan a path to it
 * and avoid obstacles.
 * 
 * @author sebastian
 */
public class NavRobot extends Pioneer
{
    /**
     * @deprecated Use {@link #NavRobot(Device[])} instead.
     * @param roboDevices
     */
	public NavRobot (Device roboDevices) { super(roboDevices); }
	
	/**
	 * Creates a navigation robot.
	 * @param devList The devices the robot can use.
	 */
	public NavRobot (Device[] devList)
	{
	    super(devList);
	}

	@Override protected void update () {	/** Robot is planner controlled */	}
	
}
