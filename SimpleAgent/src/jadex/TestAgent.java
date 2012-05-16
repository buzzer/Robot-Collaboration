package jadex;


import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Arguments;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.service.IMessageService;
import jadex.service.MessageService;

import java.lang.management.ManagementFactory;

@Arguments(@Argument(name="player path", description="This parameter is the argument given to the agent", clazz=String.class, defaultvalue="test"))
@ProvidedServices(
	@ProvidedService(type=IMessageService.class,implementation=@Implementation(expression="new MessageService(getExternalAccess())"))
	)
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestAgent extends MicroAgent {

//-------- attributes --------
	
	/** The message service. */
	//protected MessageService ms;
	
	//-------- methods --------
	
	/**
	 *  Called once after agent creation.
	 */
	public IFuture agentCreated()
	{
		//ms = new MessageService(getExternalAccess());
		//addDirectService(ms);
//		ms.tell("TestAgent", "msg from testagent");
		getMessageService().tell(ManagementFactory.getRuntimeMXBean().getName(), "msg from testagent");
		return IFuture.DONE;
	}
	
	/**
	 *  Get the chat service.
	 */
	public MessageService getMessageService()
	{
		return (MessageService) getServiceContainer().getProvidedServices(MessageService.class)[0];
	}
	
	//-------- static methods --------

//	/**
//	 *  Get the meta information about the agent.
//	 */
//	public static MicroAgentMetaInfo getMetaInfo()
//	{
//		return new MicroAgentMetaInfo("This agent starts up the Explorer agent.", 
//				null, new IArgument[]{
//				new Argument("player path", "This parameter is the argument given to the agent.", "String", 
//						"test"),	
//			}, null);
//	}

}
