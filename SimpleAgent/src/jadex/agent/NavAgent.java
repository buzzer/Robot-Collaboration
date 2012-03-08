package jadex.agent;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.*;
import jadex.service.IReceiveNewGoalService;
import jadex.service.ReceiveNewGoalService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;

import data.Host;
import data.Position;
import device.Device;
import device.DeviceNode;
import device.external.IDevice;
import device.external.ILocalizeListener;
import device.external.IPlannerListener;
import robot.NavRobot;
import jadex.service.*;

/**
 * @author sebastian
 * 
 */
@Description("This agent starts up a navigation agent.")
@Agent
@Arguments
({
		@Argument(name = "host", description = "Player", clazz = String.class, defaultvalue = "\"localhost\""),
		@Argument(name = "port", description = "Player", clazz = Integer.class, defaultvalue = "6665"),
		@Argument(name = "robID", description = "Robot identifier", clazz = Integer.class, defaultvalue = "0"),
		@Argument(name = "devIndex", description = "Device index", clazz = Integer.class, defaultvalue = "0"),
		@Argument(name = "X", description = "Meter", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "Y", description = "Meter", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "Angle", description = "Degree", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "laser", description = "Laser ranger", clazz = Boolean.class, defaultvalue = "true"),
		@Argument(name = "simulation", description = "Simulation device", clazz = Boolean.class, defaultvalue = "true")
})

@ProvidedServices
({
		@ProvidedService(type = IReceiveNewGoalService.class, implementation = @Implementation(ReceiveNewGoalService.class)),
		@ProvidedService(type = IHelloService.class, implementation = @Implementation(HelloService.class)),
		@ProvidedService(type = ISendPositionService.class, implementation = @Implementation(SendPositionService.class)),
		@ProvidedService(type = IGoalReachedService.class, implementation = @Implementation(GoalReachedService.class))
})

public class NavAgent
{
	@Agent
	MicroAgent agent;
	/** Logging support */
    static Logger logger = Logger.getLogger (NavAgent.class.getName ());
	
	DeviceNode deviceNode = null;
	NavRobot robot = null;
	
	@AgentCreated
	public IFuture<Void> agentCreated()
	{
		String host = (String)agent.getArgument("host");
		Integer port = (Integer)agent.getArgument("port");
        Integer robotIdx = (Integer)agent.getArgument("robId");
        Boolean hasLaser = (Boolean)agent.getArgument("laser");
        Boolean hasSimu = (Boolean)agent.getArgument("simulation");
        Integer devIdx = (Integer)agent.getArgument("devIndex");

        /** Device list */
        CopyOnWriteArrayList<Device> devList = new CopyOnWriteArrayList<Device>();
        devList.add( new Device(IDevice.DEVICE_POSITION2D_CODE,host,port,devIdx) ); // TODO why playerclient blocks if not present?
        if (hasSimu == true)
            devList.add( new Device(IDevice.DEVICE_SIMULATION_CODE,null,-1,-1) );
        
        devList.add( new Device(IDevice.DEVICE_PLANNER_CODE,host,port+1,devIdx) );
        devList.add( new Device(IDevice.DEVICE_LOCALIZE_CODE,host,port+1,devIdx) );

        if (hasLaser == true)
            devList.add( new Device(IDevice.DEVICE_RANGER_CODE,host,port,-1));

        /** Host list */
        CopyOnWriteArrayList<Host> hostList = new CopyOnWriteArrayList<Host>();
        hostList.add(new Host(host,port));
        hostList.add(new Host(host,port+1));
        if (port != 6665)
            hostList.add(new Host(host,6665));
        
        /** Get the device node */
        setDeviceNode( new DeviceNode(hostList.toArray(new Host[hostList.size()]), devList.toArray(new Device[devList.size()])));
		deviceNode.runThreaded();

		robot = new NavRobot(deviceNode.getDeviceListArray());
        getRobot().setRobotId("r"+robotIdx);
		
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
		return IFuture.DONE;
	}
	
	void sendHello()
	{
		//hs.send(""+getComponentIdentifier(), robot.getRobotId(), robot.getClass().getName());
		HelloService.send(""+agent.getExternalAccess(), robot.getRobotId(), robot.getClass().getName());
		logger.fine(""+agent.getExternalAccess()+" sending hello");
	}

	protected void sendPosition(Position newPose)
	{
	    if (newPose != null)
	    {
    		//ps.send(""+getComponentIdentifier(), robot.getRobotId(), newPose);
	    	this.getSendPositionService.send(""+agent.getExternalAccess(), robot.getRobotId(), newPose);
    		logger.finest(""+agent.getExternalAccess()+" sending position "+newPose);
	    }
	}
	@AgentBody
	public IFuture<Void> executeBody()
	//@Override public void executeBody()
	{
		/** Agent is worthless if underlying robot or devices fail */
		if (robot == null || deviceNode == null) {
			//TODO
//			killAgent();
		}
		
		/**
		 *  Register planner callback
		 */
		agent.scheduleStep(new IComponentStep()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if (robot.getPlanner() != null) /** Does it have a planner? */
				{
					robot.getPlanner().addIsDoneListener(new IPlannerListener()
					{
						@Override public void callWhenIsDone()
						{
							gr.send(""+getComponentIdentifier(), ""+robot,robot.getPlanner().getGoal());

							logger.fine(""+getComponentIdentifier()+" "+robot+" reached goal "+robot.getPlanner().getGoal());
						}

                        @Override public void callWhenAbort()
                        {
                            /** Set the goal again. */
//                            robot.setGoal(robot.getGoal());
                            logger.info("Aborted");
                        }

                        @Override public void callWhenNotValid()
                        {
                            logger.info("No valid path");
                        }
					});
				}
				return null;
			}
		});
		
		/**
		 *  Register localizer callback
		 */
		agent.scheduleStep(new IComponentStep()
		{
			public Object execute(IInternalAccess ia)
			{
				if (robot.getLocalizer() != null) /** Does it have a localizer? */
				{
				    /**
				     * Register a localize callback.
				     * When it is called send the new position.
				     */
					robot.getLocalizer().addListener(new ILocalizeListener()
					{
						@Override public void newPositionAvailable(Position newPose)
						{
							sendPosition(newPose);
						}
					});
				}
				else
				{
				    /**
			         * Read position periodically
			         */
			        final IComponentStep step = new IComponentStep()
			        {
			            public Object execute(IInternalAccess ia)
			            {
			                Position curPose = robot.getPosition();
			                sendPosition(curPose);
			                logger.finest("Sending new pose "+curPose+" for "+robot);
			               
			                waitFor(1000,this);
			                return null;
			            }
			        };
			        waitForTick(step);
				}
				return null;
			}
		});

		/**
		 *  Register new goal event callback
		 */
		agent.scheduleStep(new IComponentStep()
		{
			public Object execute(IInternalAccess ia)
			{
				getReceiveNewGoalService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						
						String id = (String)content[1];
						Position goal = (Position)content[2];
						logger.finer("Receiving "+id+" @ "+goal+" "+getComponentIdentifier());
						
						/** Check if it is this robot's goal */
						if (
						     id.equals(robot.getRobotId()) == true ||
							 id.equals("all") == true
						)
						{
							robot.setGoal(goal);
							logger.finest(""+robot+" received new goal "+goal);
						}
					}
				});
				return null;
			}
		});
	
		/**
		 *  Register to HelloService
		 */
		agent.scheduleStep(new IComponentStep()
		{
			public Object execute(IInternalAccess ia)
			{
				getHelloService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						String type = (String)content[2];
						
						/** Check for reply request */
						if (type.equalsIgnoreCase("ping"))
						{
							sendHello();
                            logger.finer(""+getComponentIdentifier()+" receiving "+type);
						}
					}
				});
				return null;
			}
		});
	
		/**
		 *  Register to Position update service
		 */
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				getSendPositionService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						String id = (String)content[1];
						
						/** Sending position on request */
						if (id.equals("request"))
						{
							sendPosition(robot.getPosition());
						}
					}
				});
				return null;
			}

		});
		return new Future<Void>();
	}
	
	@AgentKilled
	public IFuture<Void> agentKilled()
	{
	    robot.stop();
		robot.shutdown();
		deviceNode.shutdown();
		
		HelloService.send(""+agent.getExternalAccess(), ""+robot, "Bye");
		logger.fine("Bye "+agent.getExternalAccess());
		return IFuture.DONE;
	}
	
	public IHelloService getHelloService()
	{
		return (IHelloService) (agent.getServiceContainer()
				.getProvidedServices(IHelloService.class)[0]);
	}
	public ISendPositionService getSendPositionService()
	{
		return (ISendPositionService) (agent.getServiceContainer()
				.getProvidedServices(ISendPositionService.class)[0]);
	}
	public IReceiveNewGoalService getReceiveNewGoalService()
	{
		return (IReceiveNewGoalService) (agent.getServiceContainer()
				.getProvidedServices(IReceiveNewGoalService.class)[0]);
	}
	public IGoalReachedService getGoalReachedService()
	{
		return (IGoalReachedService) (agent.getServiceContainer()
				.getProvidedServices(IGoalReachedService.class)[0]);
	}

	public Logger getLogger() {
		return logger;
	}

    /**
     * @return the robot
     */
    protected NavRobot getRobot() {
        return robot;
    }
    protected void setRobot(NavRobot newRobot) {
    	robot = newRobot;
    }

	/**
	 * @return the deviceNode
	 */
	protected DeviceNode getDeviceNode() {
		return deviceNode;
	}

	/**
	 * @param deviceNode the deviceNode to set
	 */
	protected void setDeviceNode(DeviceNode deviceNode) {
		this.deviceNode = deviceNode;
	}
}