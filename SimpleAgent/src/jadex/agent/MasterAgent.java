package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
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

import java.util.logging.Logger;

import data.Board;
import data.BoardObject;
import data.Position;
@Agent
@ProvidedServices({ 
	@ProvidedService(type=IHelloService.class,implementation=@Implementation(HelloService.class)),
	@ProvidedService(type=ISendPositionService.class,implementation=@Implementation(SendPositionService.class))
	,@ProvidedService(type=IReceiveNewGoalService.class,implementation=@Implementation(ReceiveNewGoalService.class))
	,@ProvidedService(type=IGoalReachedService.class,implementation=@Implementation(GoalReachedService.class))	
})
public class MasterAgent extends MicroAgent
{
	/** Logging support */
    static Logger logger = Logger.getLogger (MasterAgent.class.getName ());
	@Agent
	MicroAgent agent;
//	/** Services */
//	HelloService hs;
//	SendPositionService ps;
//	ReceiveNewGoalService gs;
//	GoalReachedService gr;
//	
	/** Blackboard */
	Board board;
	
	@Override public IFuture agentCreated()
	{
		board = new Board();

//		hs = new HelloService(getExternalAccess());
//		ps = new SendPositionService(getExternalAccess());
//		gs = new ReceiveNewGoalService(getExternalAccess());
//		gr = new GoalReachedService(getExternalAccess());
//
//		addDirectService(hs);
//		addDirectService(ps);
//		addDirectService(gs);
//		addDirectService(gr);
		
		getHelloService().send(""+getComponentIdentifier(), "", "Hello");

		logger.fine(""+getComponentIdentifier()+" sending hello ");
		return IFuture.DONE;
	}

	@Override public IFuture executeBody()
	{
		
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
						StringBuffer buf = new StringBuffer();
						buf.append("[").append(content[0].toString()).append("]: ").append(content[1].toString()).append(" ").append(content[2].toString());
						
						logger.info(""+getComponentIdentifier()+" receiving "+buf);
						
						String id = (String)content[1];
						String name = (String)content[2];
						
						if (id != "" && board.getObject(id) == null) {
							BoardObject bo = new BoardObject();
							bo.setTopic(name);
							
							board.addObject(id, bo);
							logger.info(""+getComponentIdentifier()+" adding to board: "+id);
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
						StringBuffer buf = new StringBuffer();
						buf.append("[").append(content[0].toString()).append("]: ").append(content[1].toString()).append(content[2]);
						
						String id = (String)content[1];
						Position p = (Position)content[2];
						
						BoardObject bo = board.getObject(id); 
						
						if (bo != null && p != null) {
							bo.setPosition(p);
						}

						logger.finer(""+getComponentIdentifier()+" receiving "+buf);
					}
				});
				return IFuture.DONE;
			}
		});
		
		/**
		 *  Register to goal reached service
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				getGoalReachedService().addChangeListener(new IChangeListener()
				{
					public void changeOccurred(ChangeEvent event)
					{
						Object[] content = (Object[])event.getValue();
						StringBuffer buf = new StringBuffer();
						buf.append("[").append(content[0].toString()).append("]: ").append(content[1].toString()).append(" "+content[2].toString());
						
						BoardObject bo = board.getObject((String)content[1]); 
						if (bo != null) {
							Position pose = (Position)content[2];
							if (pose != null) {
								if (bo.getGoal() != null)
									bo.getGoal().setPosition(new Position(0,0,0));
							}
						}
						logger.info(""+getComponentIdentifier()+" receiving goal reached "+buf);
						
					}
				});
				return IFuture.DONE;
			}
		});
		
		return new Future();
		
//		/**
//		 * Request all robot agents.
//		 * Do it periodically.
//		 */
//		final IComponentStep step = new IComponentStep()
//		{
//			public Object execute(IInternalAccess ia)
//			{
//				pingAllAgents();
//				
//				waitFor(30000,this);
//				return null;
//			}
//		};
//		waitForTick(step);
//		
//		/** Send a 1st goal */
//		waitFor(5000, new IComponentStep()
//		{
//			public Object execute(IInternalAccess ia)
//			{
//				gs.send(getComponentIdentifier().toString(), "all", new Position(-6.5,-1.5,0));
//				return null;
//			}
//		});
		
//		final IComponentStep step = new IComponentStep()
//		{
//			public Object execute(IInternalAccess ia)
//			{
//				waitFor(1000,this);
//				return null;
//			}
//		};
//		waitForTick(step);
	
	
	}
	
	
	public void pingAllAgents()
	{
		getHelloService().send(""+getComponentIdentifier(), "", "ping");

		logger.info(""+getComponentIdentifier()+" pinging all agents");
	}
	@Override public IFuture agentKilled()
	{
		board.clear();

		getHelloService().send(""+getComponentIdentifier(), "", "Bye");

		logger.fine(""+getComponentIdentifier()+" sending bye");
		return IFuture.DONE;
	}

	public HelloService getHelloService() { return (HelloService) getServiceContainer().getProvidedServices(IHelloService.class)[0]; }
	public SendPositionService getSendPositionService() { return (SendPositionService) getServiceContainer().getProvidedServices(ISendPositionService.class)[0];}
	public ReceiveNewGoalService getReceiveNewGoalService() { return (ReceiveNewGoalService) getServiceContainer().getProvidedServices(IReceiveNewGoalService.class)[0]; }
	public GoalReachedService getGoalReachedService() { return (GoalReachedService) getServiceContainer().getProvidedServices(IGoalReachedService.class)[0]; }
	
	void goToAll(Position goalPos) {
		// TODO implement
		
	}
	void goToRobot(Position goalPos, String robotName) {
		getReceiveNewGoalService().send(agent.getExternalAccess(),""+getComponentIdentifier(), robotName, new Position(goalPos));
	}
	protected Board getBoard() {
		return board;
	}

	public Logger getLogger() {
		return logger;
	}
}
