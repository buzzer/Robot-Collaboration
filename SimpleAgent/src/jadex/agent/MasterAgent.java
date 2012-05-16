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
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MasterAgent extends MicroAgent
{
	/** Logging support */
    static Logger logger = Logger.getLogger (MasterAgent.class.getName ());

	/** Blackboard */
	Board board;
	
	@AgentCreated
	@Override public IFuture agentCreated()
	{
		board = new Board();

		
		HelloService.send(""+getComponentIdentifier(), "","Hello",getExternalAccess());
		logger.fine(""+getComponentIdentifier()+" sending hello ");
		return IFuture.DONE;
	}
	@AgentBody
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
	
	}
	
	
	public void pingAllAgents()
	{
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				//HelloService().send(""+getComponentIdentifier(), " ", "ping",getExternalAccess());
				HelloService.send(""+getComponentIdentifier(), "", "ping", getExternalAccess());
				logger.info(""+getComponentIdentifier()+" pinging all agents");
				return IFuture.DONE;
			}
		});
	}
	
	@Override
	public IFuture agentKilled()
	{
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				board.clear();
				// HelloService().send(""+getComponentIdentifier(), "", "Bye",
				// getExternalAccess());
				HelloService.send("" + getComponentIdentifier(), "", "Bye",
						getExternalAccess());
				logger.fine("" + getComponentIdentifier() + " sending bye");
				return (IFuture<Void>) IFuture.DONE;
			}
		});
		return (IFuture<Void>) IFuture.DONE;
	}

	public HelloService getHelloService()
	{
		return (HelloService) getRawService(IHelloService.class);
	}

	public SendPositionService getSendPositionService()
	{
		return (SendPositionService) getRawService(ISendPositionService.class);
	}

	public ReceiveNewGoalService getReceiveNewGoalService()
	{
		return (ReceiveNewGoalService) getRawService(IReceiveNewGoalService.class);
	}

	public GoalReachedService getGoalReachedService()
	{
		return (GoalReachedService) getRawService(IGoalReachedService.class);
	}

	void goToAll(Position goalPos) {
		// TODO implement
		
	}

	void goToRobot(final Position goalPos,final String robotName)
	{
		scheduleStep(new IComponentStep()
		{
			@SuppressWarnings("static-access")
			public IFuture execute(IInternalAccess ia)
			{
				getReceiveNewGoalService().send(getExternalAccess(),
						"" + getComponentIdentifier(), robotName,
						new Position(goalPos));

				return IFuture.DONE;
			}
		});

	}
	protected Board getBoard() {
		return board;
	}

	public Logger getLogger() {
		return logger;
	}
}
