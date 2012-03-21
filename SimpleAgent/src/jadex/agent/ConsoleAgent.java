package jadex.agent;

import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentCreated;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.service.GoalReachedService;
import jadex.service.HelloService;
import jadex.service.IGoalReachedService;
import jadex.service.IHelloService;
import jadex.service.IMessageService;
import jadex.service.IReceiveNewGoalService;
import jadex.service.ISendPositionService;
import jadex.service.MessageService;
import jadex.service.ReceiveNewGoalService;
import jadex.service.SendPositionService;
import jadex.tools.MessagePanel;

import javax.swing.SwingUtilities;

/**
 *  Message micro agent. 
 */
@Agent
@ProvidedServices({ 
	@ProvidedService(type=IMessageService.class,implementation=@Implementation(MessageService.class)),
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class)),
	@ProvidedService(type=IReceiveNewGoalService.class,implementation=@Implementation(ReceiveNewGoalService.class)), 
	@ProvidedService(type=IGoalReachedService.class,implementation=@Implementation(GoalReachedService.class))})

public class ConsoleAgent extends MicroAgent
{

	
	/**
	 *  Called once after agent creation.
	 */
	@AgentCreated
	public IFuture agentCreated()
	{

		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				MessagePanel.createGui((IMicroExternalAccess)getExternalAccess());
			}
		});
		return new Future();
	}
	/**
	 *  Get the services.
	 */
	public MessageService getMessageService() { return (MessageService) getServiceContainer().getProvidedServices(MessageService.class)[0]; 	}
	public HelloService getHelloService() { return (HelloService) getServiceContainer().getProvidedServices(HelloService.class)[0]; }
	public SendPositionService getSendPositionService() { return (SendPositionService) getServiceContainer().getProvidedServices(SendPositionService.class)[0]; }
	public ReceiveNewGoalService getReceiveNewGoalService() { return (ReceiveNewGoalService) getServiceContainer().getProvidedServices(ReceiveNewGoalService.class)[0]; }
	public GoalReachedService getGoalReachedService() { return (GoalReachedService) getServiceContainer().getProvidedServices(GoalReachedService.class)[0]; }
	
}
