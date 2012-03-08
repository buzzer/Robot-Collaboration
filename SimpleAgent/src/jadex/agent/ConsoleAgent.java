package jadex.agent;

import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.service.GoalReachedService;
import jadex.service.HelloService;
import jadex.service.IGoalReachedService;
import jadex.service.IHelloService;
import jadex.service.IReceiveNewGoalService;
import jadex.service.ISendPositionService;
import jadex.service.IMessageService;
import jadex.service.ReceiveNewGoalService;
import jadex.service.SendPositionService;
import jadex.tools.MessagePanel;
import jadex.micro.annotation.*;

import javax.swing.SwingUtilities;

/**
 * @author sebastian
 * 
 */
@Description ("Message micro agent.")
@Agent
public class ConsoleAgent
{
	//-------- attributes --------
	@Agent
	MicroAgent agent;
	
//	/** The message service. */
//	MessageService ms;
//	/** Other services */
//	HelloService hs;
//	SendPositionService ps;
//	ReceiveNewGoalService gs;
//	GoalReachedService gr;

	//-------- methods --------
	
	/**
	 *  Called once after agent creation.
	 */
	@AgentCreated
	public void agentCreated()
	{
//		ms = new MessageService(getExternalAccess());
//		hs = new HelloService(getExternalAccess());
//		ps = new SendPositionService(getExternalAccess());
//		gs = new ReceiveNewGoalService(getExternalAccess());
//		gr = new GoalReachedService(getExternalAccess());
//
//		addDirectService(ms);
//		addDirectService(hs);
//		addDirectService(ps);
//		addDirectService(gs);
//		addDirectService(gr);
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				MessagePanel.createGui((IMicroExternalAccess)getExternalAccess());
			}
		});
	}
	/**
	 *  Get the services.
	 */
	public MessageService getMessageService()
	{
		return (IMessageService) (agent.getServiceContainer()
				.getProvidedServices(IMessageService.class)[0]);
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

}
