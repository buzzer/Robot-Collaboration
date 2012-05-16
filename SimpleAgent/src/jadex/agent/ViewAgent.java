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
import jadex.service.HelloService;
import jadex.service.IHelloService;
import jadex.service.ISendPositionService;
import jadex.service.SendPositionService;

import java.util.concurrent.CopyOnWriteArrayList;

import data.Host;
import data.Position;
import device.Device;
import device.DeviceNode;
import device.Simulation;
import device.external.IDevice;
@Agent
@Arguments({
	@Argument(name="host", description="Player", clazz=String.class, defaultvalue="\"localhost\""),
	@Argument(name="port", description="Player", clazz=Integer.class, defaultvalue="6600"),
	@Argument(name="robID", description="Only track this", clazz=Integer.class, defaultvalue="-1")
})
@ProvidedServices({ 
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class))
})

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ViewAgent extends MicroAgent
{
	/** API to the simulator (gui) */
	protected Simulation simu = null;
	protected DeviceNode deviceNode = null;
	
	/** Dedicated follow robot, if any */
	String folRobot = null;
	
	@Override 
	@AgentCreated
	public IFuture agentCreated()
	{
		String host = (String)getArgument("host");
        Integer port = (Integer)getArgument("port");
        int id = (Integer)getArgument("robID");
        if (id != -1)
            folRobot = "r"+id;

		/** Device list */
        CopyOnWriteArrayList<Device> devList = new CopyOnWriteArrayList<Device>();
        devList.add( new Device(IDevice.DEVICE_SIMULATION_CODE,host,port,-1) );

        /** Host list */
        CopyOnWriteArrayList<Host> hostList = new CopyOnWriteArrayList<Host>();
        hostList.add(new Host(host,port));
        
        /** Get the device node */
        deviceNode = new DeviceNode(hostList.toArray(new Host[hostList.size()]), devList.toArray(new Device[devList.size()]));
		deviceNode.runThreaded();
		
		HelloService.send(""+getComponentIdentifier(), "","Hello", getExternalAccess());
		simu = (Simulation) deviceNode.getDevice(new Device(IDevice.DEVICE_SIMULATION_CODE, null, -1, -1));
		
		if (simu == null)
		    throw new IllegalStateException("No simulation device found");
		return IFuture.DONE;
	}

	@Override 
	@AgentBody
	public IFuture executeBody()
	{
	    scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				getSendPositionService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						
						String robId = (String)content[1];
						Position newPose = (Position)content[2];

						if (folRobot == null)
						{
						    simu.setPositionOf(robId, newPose);
						}
						else
						{
						    if (robId.equals(folRobot))
						    {
						        simu.setPositionOf(robId, newPose);
						    }
						}
					}
				});
				return IFuture.DONE;
			}
		});


	    return new Future();
	}

	@Override
	@AgentKilled
	public IFuture agentKilled()
	{

		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{

				deviceNode.shutdown();
				HelloService.send("" + getComponentIdentifier().toString(), "",
						"Bye", getExternalAccess());
				return IFuture.DONE;
			}
		});
		
		return IFuture.DONE;
	}

	public HelloService getHelloService()
	{
		return (HelloService) getRawService(IHelloService.class);
	}
	
	public SendPositionService getSendPositionService()
	{
		return (SendPositionService) getRawService(ISendPositionService.class);
	}
}
