package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.modelinfo.IArgument;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.IFuture;
import jadex.commons.future.Future;
import jadex.micro.MicroAgent;
import jadex.micro.MicroAgentMetaInfo;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Argument;
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

@Arguments({
	@Argument(name="host", description="Player", clazz=String.class, defaultvalue="localhost"),
	@Argument(name="port", description="Player", clazz=Integer.class, defaultvalue="6600"),
	@Argument(name="robID", description="Only track this", clazz=Integer.class, defaultvalue="-1")
})
@ProvidedServices({ 
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class))})
public class ViewAgent extends MicroAgent
{

	
	/** API to the simulator (gui) */
	protected Simulation simu = null;
	protected DeviceNode deviceNode = null;
	
	/** Dedicated follow robot, if any */
	String folRobot = null;
	
	@Override public IFuture agentCreated()
	{
//		hs = new HelloService(getExternalAccess());
//		ps = new SendPositionService(getExternalAccess());
//
//		addDirectService(hs);
//		addDirectService(ps);

		String host = (String)getArgument("host");
        Integer port = (Integer)getArgument("port");
        int id = (Integer)getArgument("robId");
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
		
		getHelloService().send(""+getComponentIdentifier(), "", "Hello");

		simu = (Simulation) deviceNode.getDevice(new Device(IDevice.DEVICE_SIMULATION_CODE, null, -1, -1));
		
		if (simu == null)
		    throw new IllegalStateException("No simulation device found");
		return IFuture.DONE;
	}

	@Override public IFuture executeBody()
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

//		waitFor(200, new IComponentStep()
//		{
//			public Object execute(IInternalAccess args)
//			{
//				simu = (Simulation) deviceNode.getDevice(new Device(IDevice.DEVICE_SIMULATION_CODE, null, -1, -1));
//				simu.initPositionOf("r0");
//				simu.initPositionOf("r1");
//				return null;
//			}
//		});
	    return new Future();
	}
	@Override public IFuture agentKilled()
	{
		deviceNode.shutdown();
		getHelloService().send(getComponentIdentifier().toString(), "", "Bye");
		return IFuture.DONE;
	}
//	public static MicroAgentMetaInfo getMetaInfo()
//	{
//		IArgument[] args = {
//                new Argument("host", "Player", "String", "localhost"),
//				new Argument("port", "Player", "Integer", new Integer(6600)),
//                new Argument("robId", "Only track this", "Integer", new Integer(-1))
//		};
//		
//		return new MicroAgentMetaInfo("This agent starts up a view agent.", null, args, null);
//	}

	public HelloService getHelloService() { return (HelloService) getServiceContainer().getProvidedServices(HelloService.class)[0]; }
	public SendPositionService getSendPositionService() { return (SendPositionService) getServiceContainer().getProvidedServices(SendPositionService.class)[0]; }
}
