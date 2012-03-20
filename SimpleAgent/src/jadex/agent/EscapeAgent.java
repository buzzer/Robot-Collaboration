/**
 * 
 */
package jadex.agent;

import jadex.bridge.IComponentStep;
import jadex.commons.future.Future;
import jadex.bridge.IInternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

import java.util.Timer;
import java.util.TimerTask;

import robot.external.IPioneer;
import data.Position;
import device.external.ILocalizeListener;

/**
 * @author sebastian
 *
 */

@Arguments({
	@Argument(name="minDistance", description="Minimum escape distance (m)", clazz=Double.class, defaultvalue="1.5"),
	@Argument(name="localize", description="Localize device",clazz=Boolean.class, defaultvalue="true"),
	@Argument(name="laser", description="Laser ranger", clazz=Boolean.class, defaultvalue="true"),
	@Argument(name="Angle", description="Degree", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="Y", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="X", description="Meter", clazz=Double.class, defaultvalue="0.0"),
	@Argument(name="devIndex", description="Device Index", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="robId", description="Robot identifier", clazz=Integer.class, defaultvalue="0"),
	@Argument(name="host", description="Player", clazz=String.class, defaultvalue="localhost"),
	@Argument(name="port", description="Player", clazz=Integer.class, defaultvalue="6665")
})
public class EscapeAgent extends WallfollowAgent
{
    /** Minimum escape distance */
    double minDist;
    boolean gotCaught = false;
    
    /**
     * @see jadex.agent.WallfollowAgent#agentCreated()
     */
    @Override public IFuture agentCreated()
    {
       minDist = (Double)getArgument("minDistance");
       return super.agentCreated();
    }

    /**
     * @see jadex.agent.WallfollowAgent#executeBody()
     */
    @Override public IFuture executeBody()
    {
        /**
         *  Register localizer callback
         */
        scheduleStep(new IComponentStep()
        {
            public IFuture execute(IInternalAccess ia)
            {
                if (robot.getLocalizer() != null) /** Does it have a localizer? */
                {
                    robot.getLocalizer().addListener(new ILocalizeListener()
                    {
                        @Override public void newPositionAvailable(Position newPose)
                        {
                            sendPosition(newPose);
                        }
                    });
                }
                return IFuture.DONE;
            }
        });
   
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
                                                
                        /** Check for reply request */
                        if (((String)content[2]).equalsIgnoreCase("ping"))
                        {
                            sendHello();
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
                        String id = ((String)content[1]);
                        
                        /** Sending position on request */
                        if (id.equals("request"))
                        {
                            sendPosition(getRobot().getPosition());
                        }
                        else
                        {
                            if (id.equals(getRobot().getRobotId()) == false)
                            {
                                Position folPose = (Position)content[2];
                                if (folPose != null)
                                {
                                    double folDist = folPose.distanceTo(getRobot().getPosition());
                                    getLogger().fine("Nearest follower "+folDist);
                                    
                                    if (folDist < minDist && gotCaught == false)
                                    {
                                        /** Got caught */
                                        gotCaught = true;
                                        getRobot().stop();
                                        getLogger().info("Got caught "+getRobot()+" by "+id);
                                        dance();
                                    }
                                }
                            }
                        }
                    }
                });
                return IFuture.DONE;
            }
        });
        
        /**
         * Init wall following
         */
        scheduleStep(new IComponentStep()
        {
            public IFuture execute(IInternalAccess ia)
            {
                robot.setWallfollow();
                robot.runThreaded();
                return IFuture.DONE;
            }
        });
        
        return new Future();
    }

    /**
     * @see jadex.agent.WallfollowAgent#agentKilled()
     */
    @Override public IFuture agentKilled()
    {
    	//TODO kill timer here if not yet expired
        return super.agentKilled();
    }
//
//    public static MicroAgentMetaInfo getMetaInfo()
//    {
//        IArgument[] args = {
//                new Argument("host", "Player", "String", "localhost"),
//                new Argument("port", "Player", "Integer", new Integer(6665)),
//                new Argument("robId", "Robot identifier", "Integer", new Integer(0)),
//                new Argument("devIndex", "Device index", "Integer", new Integer(0)),
//                new Argument("X", "Meter", "Double", new Double(0.0)),
//                new Argument("Y", "Meter", "Double", new Double(0.0)),
//                new Argument("Angle", "Degree", "Double", new Double(0.0)),
//                new Argument("laser", "Laser ranger", "Boolean", new Boolean(true)),
//                new Argument("localize", "Localize device", "Boolean", new Boolean(true)),
//                new Argument("minDistance", "Minimum escape distance (m)", "Double", new Double(1.5))
//        };
//        
//        return new MicroAgentMetaInfo("This agent starts up an escape agent.", null, args, null);
//    }
    void dance()
    {
    	logger.info("Dancing..");
    	getRobot().setCommand();
    	getRobot().setTurnrate(Math.toRadians(IPioneer.MAXTURNRATE));
    	Timer timer = new Timer();
    	timer.schedule(new TimerTask()
    	{
			@Override public void run() {
				getRobot().stop();
			}
    	}, 15000);
    }
}
