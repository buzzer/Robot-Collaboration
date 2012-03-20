/**
 * 
 */
package jadex.agent;


import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.service.GoalReachedService;
import jadex.service.HelloService;
import jadex.service.IGoalReachedService;
import jadex.service.IHelloService;
import jadex.service.IReceiveNewGoalService;
import jadex.service.ISendPositionService;
import jadex.service.ReceiveNewGoalService;
import jadex.service.SendPositionService;
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
	@Argument(name="host", description="Robot host", clazz=String.class, defaultvalue="localhost"),
	@Argument(name="port", description="Robot port", clazz=Integer.class, defaultvalue="6665"),
	@Argument(name="robID", description="Robot identifier", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="X", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Y", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Angle", description="Degree", clazz=Double.class, defaultvalue="0.0")
	})
@ProvidedServices({ 
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class)),
	@ProvidedService(type=IReceiveNewGoalService.class,implementation=@Implementation(ReceiveNewGoalService.class)),
	@ProvidedService(type=IGoalReachedService.class,implementation=@Implementation(GoalReachedService.class))
})
public class SwarmAgent extends NavAgent
{
    @Override public IFuture agentCreated()
    {
//        hs = new HelloService(getExternalAccess());
//        ps = new SendPositionService(getExternalAccess());
//        gs = new ReceiveNewGoalService(getExternalAccess());
//        gr = new GoalReachedService(getExternalAccess());
//
//        addDirectService(hs);
//        addDirectService(ps);
//        addDirectService(gs);
//        addDirectService(gr);

        String host = (String)agent.getArgument("host"); 
        Integer port = (Integer)agent.getArgument("port");
        Integer robotIdx = (Integer)agent.getArgument("robId");
        
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
                (Double)agent.getArgument("X"),
                (Double)agent.getArgument("Y"),
                (Double)agent.getArgument("Angle"));
        
        if ( setPose.equals(new Position(0,0,0)) == false )
            robot.setPosition(setPose);         

        sendHello();
        return new Future();
    }
   
//    public static MicroAgentMetaInfo getMetaInfo()
//    {
//        IArgument[] args = {
//                new Argument("host", "Robot host", "String", "localhost"),
//                new Argument("port", "Robot port", "Integer", new Integer(6665)),
//                new Argument("robId", "Robot identifier", "Integer", new Integer(0)),
//                new Argument("X", "Meter", "Double", new Double(0.0)),
//                new Argument("Y", "Meter", "Double", new Double(0.0)),
//                new Argument("Angle", "Degree", "Double", new Double(0.0))
//        };
//        
//        return new MicroAgentMetaInfo("This agent starts up a swarm agent.", null, args, null);
//    }
}
