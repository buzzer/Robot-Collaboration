/**
 * 
 */
package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import data.Position;
import device.external.ILocalizeListener;

/**
 * @author sebastian
 *
 */
@Agent
@Arguments({
	@Argument(name="robot",description="To follow", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="host", description="Player", clazz=String.class, defaultvalue="localhost"),
	@Argument(name="port", description="Player", clazz=Integer.class, defaultvalue="6667"),
	@Argument(name="robId", description="Robot identifier", clazz=Integer.class, defaultvalue="1"),
	@Argument(name="devIndex", description="Device Index", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="X", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Y", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Angle", description="Degree", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="laser", description="Laser ranger", clazz=Boolean.class, defaultvalue="true"),
	@Argument(name="simulation", description="Simulation device", clazz=Boolean.class, defaultvalue="true"),	
	@Argument(name="minDistance", description="Meter", clazz=Double.class, defaultvalue="2.0"),
	@Argument(name="updateInterval", description="ms", clazz=Integer.class, defaultvalue="5000")
})
public class FollowAgent extends NavAgent
{
    Position followPose;
    Position robotPose;
    boolean isNewFollowPose;
    String folRobot;
    boolean caughtRobot = false;
    long updateInterval;
    double minToGoalDist;
@Agent
MicroAgent agent;
    
    /**
     * @see jadex.agent.NavAgent#agentCreated()
     */
    @Override public IFuture agentCreated()
    {
               
        isNewFollowPose = false;
        followPose = getRobot().getPosition();
        robotPose = getRobot().getPosition();
        folRobot = "r"+(Integer)agent.getArgument("robot");
        updateInterval = (Integer)agent.getArgument("updateInterval");
        minToGoalDist = (Double)agent.getArgument("minDistance");
        super.agentCreated();
        return IFuture.DONE;
    }

    /**
     * @see jadex.agent.NavAgent#executeBody()
     */
    @Override public IFuture executeBody()
    {
        super.executeBody();

        /**
         *  Register to Position update service
         */
        agent.scheduleStep(new IComponentStep()
        {
            public IFuture execute(IInternalAccess ia)
            {
                getSendPositionService().addChangeListener(new IChangeListener()
                {
                    public void changeOccurred(ChangeEvent event)
                    {
                        Object[] content = (Object[])event.getValue();

                        /** Sending position on request */
                        if (((String)content[1]).equals( folRobot ))
                        {
                            Position curPose = (Position) content[2];
                            
                            /** Check for new position */
                            if (followPose.equals(curPose) == false)
                            {
                                followPose = curPose;
                                isNewFollowPose = true;
                            }
                        }
                    }
                });
                return IFuture.DONE;
            }
        });
        /**
         *  Register localizer callback
         */
        agent.scheduleStep(new IComponentStep()
        {
            public IFuture execute(IInternalAccess ia)
            {
                if (getRobot().getLocalizer() != null) /** Does it have a localizer? */
                {
                    getRobot().getLocalizer().addListener(new ILocalizeListener()
                    {
                        @Override public void newPositionAvailable(Position newPose)
                        {
                            robotPose = newPose;
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
                            robotPose = curPose;

                            agent.waitFor(1000,this);
                            return IFuture.DONE;
                        }
                    };
                    agent.waitForTick(step);
                }
                return IFuture.DONE;
            }
        });
        /**
         * Update goal periodically
         */
        final IComponentStep step = new IComponentStep()
        {
            public IFuture execute(IInternalAccess ia)
            {
                updateGoal();
                agent.waitFor(updateInterval,this);
                return IFuture.DONE;
            }
        };
        agent.waitForTick(step);
        return new Future();
    }
   
    void updateGoal()
    {
        /**
         * Check for distance to goal.
         */
        if (robotPose.distanceTo( followPose ) > minToGoalDist)
        {
            caughtRobot = false;
            /**
             *  Check for new goal
             */
            if (isNewFollowPose == true)
            {
                isNewFollowPose = false;
                getRobot().setGoal( followPose );
                
                logger.fine("Resume following robot "+folRobot);
                logger.finer("pose: "+robotPose+", robot pose: "+followPose);
            }
        }
        else
        {
            /**
             * Goal reached.
             */
            if (caughtRobot == false)
            {
                caughtRobot = true;
                getRobot().stop();
                logger.info("Caught robot "+folRobot);
            }
            else
            {
                /** Send position continuously, so that escape robot recognizes */
            	sendPosition(getRobot().getPosition());
            }
        }
    }

    /**
     * @see jadex.agent.NavAgent#agentKilled()
     */
    @Override public IFuture agentKilled()
    {
        super.agentKilled();
        return IFuture.DONE;
    }


    /**
     * @return the followPose
     */
    synchronized Position getFollowPose()
    {
        assert(followPose != null);
        return followPose;
    }

    /**
     * @param newFollowPose the followPose to set
     */
    synchronized void setFollowPose(Position newFollowPose) {
        assert(newFollowPose != null);
        followPose.setPosition( newFollowPose );
    }
}
