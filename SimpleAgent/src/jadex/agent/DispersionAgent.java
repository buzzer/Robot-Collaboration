/**
 * 
 */
package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

import java.util.ArrayList;
import java.util.Collections;

import robot.Robot;
import data.BoardObject;
import data.Position;

/**
 * @author sebastian
 *
 */
@Agent
@Arguments({
	@Argument(name="pingInterval", description="Time between pings in ms", clazz=Integer.class, defaultvalue="30000"),
	@Argument(name="dispersionInterval", description="Time between dispersions in ms", clazz=Integer.class, defaultvalue="60000")
})
public class DispersionAgent extends MasterAgent
{

	Integer dispersionInterval;
	
	/**
	 *  Strategic important points on the map.
	 *  Highest assign priority is at top of the list. 
	 */
	// TODO Outsource in file
	Position[] dispersionPoints =
	{
			new Position(-21,4,0), /** Top */
			new Position(-29,-1,0), /** Left */
			new Position(-22,-4,0), /** Bottom */
			new Position(-22,-1.5,0), /** Center */
			new Position(-14,-1.5,0), /** Center right */
			new Position(-3,-1,0) /** Right */
	};

	@Override public IFuture agentCreated()
	{
		super.agentCreated();
		
		dispersionInterval = (Integer)getArgument("dispersionInterval");
		return IFuture.DONE;
	}
	@Override public IFuture executeBody()
	{
		super.executeBody();

		/**
		 * Request all available robot agents to respond.
		 * Do it periodically.
		 */
		scheduleStep(new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				//				getBoard().clear();
				pingAllAgents();

				waitFor((Integer)getArgument("pingInterval"),this);
				return IFuture.DONE;
			}
		});

		/**
		 * Request all positions of available robot agents.
		 * Wait a little so that all robots have a true ground pose.
		 */
//		scheduleStep(new IComponentStep()
		waitFor(1000, new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				requestAllPositions();
				return IFuture.DONE;
			}
		});

		/**
		 *  Assign goal positions
		 */
//		scheduleStep(new IComponentStep()
		waitFor(1100, new IComponentStep()
		{
			public IFuture execute(IInternalAccess ia)
			{
				ArrayList<Integer> positions = new ArrayList<Integer>();

				/** Create a sorted number array */
				for (int i=0; i<dispersionPoints.length; i++)
				{
					positions.add(i);
				}
				
				/** Get all robots */
				ArrayList<String> robotKeys = getBoard().getTopicList(Robot.class.getName());
				/** Randomize robot goal assign order */
				Collections.shuffle(robotKeys);
				
				getLogger().finer("Shuffle robots: "+robotKeys);

				/**
				 * Assign a goal to each robot
				 */
				for (int i=0; i<robotKeys.size(); i++)
				{
					int goalIndex = -1;
					Position nearestGoal = null;
					Position curGoal = null;

					/** Get the robot board object */
					BoardObject bo = getBoard().getObject(robotKeys.get(i)); 

					/** Check for the robot distance to goal */
					if (bo != null)
					{
						Position robotPose = bo.getPosition();

						if (robotPose != null)
						{
							getLogger().finer(robotKeys.get(i)+" pose: "+robotPose);

							double minGoalDistance = Double.MAX_VALUE;

							for (int i1=0; i1<positions.size(); i1++)
							{
								curGoal = dispersionPoints[positions.get(i1)];
								logger.finer("Checking for goal: "+curGoal);

								double robotDist = robotPose.distanceTo(curGoal);
								logger.finer("Distance: "+robotDist);

								if (minGoalDistance > robotDist)
								{
									minGoalDistance = robotDist;
									goalIndex = i1;
									nearestGoal = curGoal;
								}
							}
						}
						else
						{
							logger.info("Robot pose null from: "+bo);
						}
					}
					else
					{
						logger.info("Board object null from key: "+robotKeys.get(i));
					}

					/** Did we found an appropriate robot goal */
					if (nearestGoal != null)
					{
						getLogger().finer("Nearest goal is "+nearestGoal+" index: "+positions.get(goalIndex));
						
						getReceiveNewGoalService().send(agent.getExternalAccess(),""+getComponentIdentifier(), robotKeys.get(i), nearestGoal);

						getLogger().finer("Sending goal: "+nearestGoal+" to "+robotKeys.get(i));
					
						/** Remove goal from list */
						positions.remove(goalIndex);
					}

				}

				if (dispersionInterval != -1)
				{
					waitFor(dispersionInterval,this);
				}
				else
				{
					killAgent();
				}

				return IFuture.DONE;
			}
		});
		
		return new Future();
	}
	protected void requestAllPositions()
	{
		getSendPositionService().send(""+getComponentIdentifier(), "request", null);

		getLogger().info(""+getComponentIdentifier()+" requesting all positions");		
	}
	@Override public IFuture agentKilled()
	{
		super.agentKilled();
		return IFuture.DONE;
	}


}
