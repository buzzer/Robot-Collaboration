/**
 * 
 */
package jadex.agent;


import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import robot.NavRobot;
import data.Host;
import data.Position;
import device.Device;
import device.DeviceNode;
import device.external.IDevice;

/**
 * @author sebastian
 *
 */
@Agent
@Arguments({
	@Argument(name="host", description="Robot host", clazz=String.class, defaultvalue="\"localhost\""),
	@Argument(name="port", description="Robot port", clazz=Integer.class, defaultvalue="6665"),
	@Argument(name="robID", description="Robot identifier", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="X", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Y", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Angle", description="Degree", clazz=Double.class, defaultvalue="0.0")
	})

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SwarmAgent extends NavAgent
{
    @Override 
    @AgentCreated
    public IFuture agentCreated()
    {
        String host = (String)getArgument("host"); 
        Integer port = (Integer)getArgument("port");
        Integer robotIdx = (Integer)getArgument("robID");
        
        /** Get the device node */
        deviceNode = new DeviceNode(
            new Host[]
            {
                new Host(host,port),
                new Host(host,port+1)
            },
            new Device[]
            {
                new Device(IDevice.DEVICE_POSITION2D_CODE,host,port,robotIdx),
                new Device(IDevice.DEVICE_PLANNER_CODE,host,port+1,robotIdx),
                new Device(IDevice.DEVICE_SIMULATION_CODE,host,port,-1)
            });
        
        deviceNode.runThreaded();

        robot = new NavRobot(deviceNode.getDeviceListArray());
        robot.setRobotId("r"+robotIdx);
        
        /**
         *  Check if a particular position is set
         */
        Position setPose = new Position(
                (Double)getArgument("X"),
                (Double)getArgument("Y"),
                (Double)getArgument("Angle"));
        
        if ( setPose.equals(new Position(0,0,0)) == false )
            robot.setPosition(setPose);         

        sendHello();
        return new Future();
    }
}
