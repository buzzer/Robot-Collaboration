package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.service.HelloService;
import jadex.service.IHelloService;
import jadex.service.IReceiveNewGoalService;
import jadex.service.ISendPositionService;
import jadex.service.ReceiveNewGoalService;
import jadex.service.SendPositionService;

import java.util.concurrent.CopyOnWriteArrayList;

import robot.ExploreRobot;
import data.BlobfinderBlob;
import data.Board;
import data.BoardObject;
import data.Host;
import data.Position;
import device.Device;
import device.DeviceNode;
import device.external.IBlobfinderListener;
import device.external.IDevice;

@Agent
@Arguments(
{
		@Argument(name = "host", description = "Player", clazz = String.class, defaultvalue = "localhost"),
		@Argument(name = "port", description = "Player", clazz = Integer.class, defaultvalue = "6665"),
		@Argument(name = "robId", description = "Robot identifier", clazz = Integer.class, defaultvalue = "0"),
		@Argument(name = "devIndex", description = "Device index", clazz = Integer.class, defaultvalue = "0"),
		@Argument(name = "X", description = "Meter", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "Y", description = "Meter", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "Angle", description = "Degree", clazz = Double.class, defaultvalue = "0.0"),
		@Argument(name = "laser", description = "Laser ranger", clazz = Boolean.class, defaultvalue = "true"),
		@Argument(name = "localize", description = "Localize device", clazz = Boolean.class, defaultvalue = "true") })
@ProvidedServices(
{
	
	
//		@ProvidedService(type = IHelloService.class, implementation = @Implementation(HelloService.class)),
//		@ProvidedService(type = ISendPositionService.class, implementation = @Implementation(SendPositionService.class)),
		@ProvidedService(type = IReceiveNewGoalService.class, implementation = @Implementation(ReceiveNewGoalService.class)) })
public class ExploreAgent extends WallfollowAgent
{
	
	@Agent
	MicroAgent agent;
	/** Data */
	Board bb;

	@Override
	public IFuture agentCreated()
	{
		
		String host = (String) getArgument("host");
		Integer port = (Integer) getArgument("port");
		Integer robotIdx = (Integer) getArgument("robId");
		Integer devIdx = (Integer) getArgument("devIndex");
		Boolean hasLaser = (Boolean) getArgument("laser");

		/** Device list */
		CopyOnWriteArrayList<Device> devList = new CopyOnWriteArrayList<Device>();
		devList.add(new Device(IDevice.DEVICE_POSITION2D_CODE, host, port,
				devIdx));
		devList.add(new Device(IDevice.DEVICE_RANGER_CODE, host, port, devIdx));
		devList.add(new Device(IDevice.DEVICE_SONAR_CODE, host, port, devIdx));
		devList.add(new Device(IDevice.DEVICE_BLOBFINDER_CODE, host, port,
				devIdx));
		devList.add(new Device(IDevice.DEVICE_SIMULATION_CODE, host, port, -1));
		devList.add(new Device(IDevice.DEVICE_SIMULATION_CODE, host, 6665, -1));
		devList.add(new Device(IDevice.DEVICE_PLANNER_CODE, host, port + 1,
				devIdx));
		devList.add(new Device(IDevice.DEVICE_LOCALIZE_CODE, host, port + 1,
				devIdx));

		if (hasLaser == true)
			devList.add(new Device(IDevice.DEVICE_RANGER_CODE, host, port,
					devIdx + 1));

		/** Host list */
		CopyOnWriteArrayList<Host> hostList = new CopyOnWriteArrayList<Host>();
		hostList.add(new Host(host, 6665));
		hostList.add(new Host(host, port));
		hostList.add(new Host(host, port + 1));

		/** Get the device node */
		setDeviceNode(new DeviceNode(hostList
				.toArray(new Host[hostList.size()]), devList
				.toArray(new Device[devList.size()])));
		getDeviceNode().runThreaded();

		setRobot(new ExploreRobot(getDeviceNode().getDeviceListArray()));
		getRobot().setRobotId("r" + robotIdx);

		/**
		 * Check if a particular position is set
		 */
		Position setPose = new Position((Double) getArgument("X"),
				(Double) getArgument("Y"), (Double) getArgument("Angle"));

		if (setPose.equals(new Position(0, 0, 0)) == false)
			getRobot().setPosition(setPose);

		bb = new Board();

		sendHello();
		return IFuture.DONE;
	}

	/**
	 * @see jadex.agent.WallfollowAgent#executeBody()
	 */
	@Override
	public IFuture executeBody()
	{
		super.executeBody();

		/**
		 * Register to Blobfinder device
		 */
		if (getRobot().getBloFi() != null)
		{
			scheduleStep(new IComponentStep()
			{
				public IFuture execute(IInternalAccess ia)
				{
					getRobot().getBloFi().addBlobListener(
							new IBlobfinderListener()
							{
								@Override
								public void newBlobFound(BlobfinderBlob newBlob)
								{
									/** Board object */
									if (bb.getObject(newBlob.getColorString()) == null)
									{
						
										Position globPose = getRobot()
												.getPosition();
										System.out.println("Rob pose: "
												+ globPose);
										// Transform to robot camera coordinates
										double camRange = 2.0; // meter
										globPose.setX(globPose.getX()
												+ Math.cos(globPose.getYaw())
												* camRange);
										globPose.setY(globPose.getY()
												+ Math.sin(globPose.getYaw())
												* camRange);

										// Create board object
										BoardObject bo = new BoardObject();
										bo.setTopic("" + newBlob.getClass());
										bo.setPosition(globPose);
										bo.setTimeout(10000);

										bb.addObject(newBlob.getColorString(),
												bo);
										sendBlobInfo(bo);
									}
								}
							});
					return IFuture.DONE;
				}
			});
		}
		return new Future();
	}

	/**
	 * Send a new goal locating a {@link BoardObject}
	 * 
	 * @param bo
	 *            The board object to be found
	 */
	void sendBlobInfo(BoardObject bo)
	{
		getReceiveNewGoalService().send(agent.getExternalAccess(),"" + getComponentIdentifier(),
				"collectGoal", bo.getPosition());
		logger.info("Sending blob info from " + bo);
	}

	public ReceiveNewGoalService getReceiveNewGoalService()
	{
		return (ReceiveNewGoalService) getServiceContainer().getProvidedServices(ReceiveNewGoalService.class)[0];
	}


}
