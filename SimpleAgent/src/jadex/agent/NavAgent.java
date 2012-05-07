package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.AgentKilled;
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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import robot.NavRobot;
import data.Host;
import data.Position;
import device.Device;
import device.DeviceNode;
import device.external.IDevice;
import device.external.ILocalizeListener;
import device.external.IPlannerListener;

@Agent
@Arguments({
	@Argument(name="host", description="Player", clazz=String.class, defaultvalue="\"localhost\""),
	@Argument(name="port", description="Player", clazz=Integer.class, defaultvalue="6665"),
	@Argument(name="robID", description="Robot identifier", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="devIndex", description="Device Index", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="X", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Y", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Angle", description="Degree", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="laser", description="Laser ranger", clazz=Boolean.class, defaultvalue="true"),
	@Argument(name="simulation", description="Simulation device", clazz=Boolean.class, defaultvalue="true")	
})
@ProvidedServices({ 
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class)),
	@ProvidedService(type=IReceiveNewGoalService.class,implementation=@Implementation(ReceiveNewGoalService.class)),
	@ProvidedService(type=IGoalReachedService.class,implementation=@Implementation(GoalReachedService.class))	
})
public class NavAgent extends MicroAgent
{
	/** Logging support */
    static Logger logger = Logger.getLogger (NavAgent.class.getName ());

    @Agent
//    MicroAgent agent;
	DeviceNode deviceNode = null;
	NavRobot robot = null;
	
	@Override
	@AgentCreated
	 public IFuture agentCreated()
	{

		String host = (String) getArgument("host");
		Integer port = (Integer) getArgument("port");
        Integer robotIdx = (Integer) getArgument("robID");
        Boolean hasLaser = (Boolean) getArgument("laser");
        Boolean hasSimu = (Boolean) getArgument("simulation");
        Integer devIdx = (Integer) getArgument("devIndex");

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
                (Double)getArgument("X"),
                (Double)getArgument("Y"),
                (Double)getArgument("Angle"));
		
		if ( setPose.equals(new Position(0,0,0)) == false )
		    robot.setPosition(setPose);		    

		sendHello();
		return IFuture.DONE;
	}
	
	void sendHello()
	{
		//HelloService().send(""+ agent.getComponentIdentifier(), robot.getRobotId(), robot.getClass().getName());
		HelloService.send(""+getComponentIdentifier(), ""+robot.getRobotId(), robot.getClass().getName(), getExternalAccess());
		logger.fine(""+ getComponentIdentifier()+" sending hello");
	}

	protected void sendPosition(Position newPose)
	{
	    if (newPose != null)
	    {
    		getSendPositionService().send(""+ getComponentIdentifier(), robot.getRobotId(), newPose);
    		logger.finest(""+ getComponentIdentifier()+" sending position "+newPose);
	    }
	}
	
	@AgentBody
	 public IFuture executeBody()
	{
		/** Agent is worthless if underlying robot or devices fail */
		if (robot == null || deviceNode == null) {
			//killAgent();
			return IFuture.DONE;
		}
		
		/**
		 *  Register planner callback
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				if (robot.getPlanner() != null) /** Does it have a planner? */
				{
					robot.getPlanner().addIsDoneListener(new IPlannerListener()
					{
						@Override public void callWhenIsDone()
						{
							getGoalReachedService().send(""+getComponentIdentifier(), ""+robot,robot.getPlanner().getGoal());

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
			 return IFuture.DONE;
			}
		});
		
		/**
		 *  Register localizer callback
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
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
			            public IFuture execute(IInternalAccess ia)
			            {
			                Position curPose = robot.getPosition();
			                sendPosition(curPose);
			                logger.finest("Sending new pose "+curPose+" for "+robot);
			               
			                waitFor(1000,this);
			                return IFuture.DONE;
			            }
			        };
			        waitForTick(step);
				}
				return IFuture.DONE;
			}
		});

		/**
		 *  Register new goal event callback
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				getReceiveNewGoalService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						
						String id = (String)content[1];
						Position goal = (Position)content[2];
						logger.finer("Receiving "+id+" @ "+goal+" "+ getComponentIdentifier());
						
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
				return IFuture.DONE;
			}
		});
	
		/**
		 *  Register to HelloService
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
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
                            logger.finer(""+ getComponentIdentifier()+" receiving "+type);
						}
					}
				});
				return IFuture.DONE;
			}
		});
	
		/**
		 *  Register to Position update service
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
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
				return IFuture.DONE;
			}
		});
		return new Future();
	}
	
	@AgentKilled
	public IFuture agentKilled()
	{
	    robot.stop();
		robot.shutdown();
		deviceNode.shutdown();
		
		//getHelloService().send(""+ agent.getComponentIdentifier(), ""+robot, "Bye");
		HelloService.send(""+getComponentIdentifier(), ""+robot, "Bye", getExternalAccess());
		
		logger.fine("Bye "+ getComponentIdentifier());
		return IFuture.DONE;
		
	}
	
	public HelloService getHelloService() { return (HelloService) getRawService(IHelloService.class); }
	public SendPositionService getSendPositionService() { return (SendPositionService) getRawService(ISendPositionService.class); }
	public ReceiveNewGoalService getReceiveNewGoalService() { return (ReceiveNewGoalService) getRawService(IReceiveNewGoalService.class); }
	public GoalReachedService getGoalReachedService() { return (GoalReachedService) getRawService(IGoalReachedService.class); }


	
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