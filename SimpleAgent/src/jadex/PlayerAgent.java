package jadex;

import jadex.commons.future.IFuture;
import jadex.commons.future.Future;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;
import java.util.logging.Logger;
import core.OSCommand;
import core.ProjectLogger;


@Arguments({
	@Argument(name="killall path",clazz=String.class, defaultvalue="\"/usr/bin/killall\""),
	@Argument(name="process name",clazz=String.class,defaultvalue="\"player\""),
	@Argument(name="requires player", description="dummy", clazz=Boolean.class, defaultvalue="false"),
	@Argument(name="player path", description="dummy",clazz=String.class,defaultvalue=" playerPath"),
	@Argument(name="player port", description="dummy",clazz=Integer.class,defaultvalue="port"),
	@Argument(name="player config", description= "dummy", clazz= String.class, defaultvalue="\"/Users/sebastian/robotcolla/SimpleAgent/player/uhh1.cfg\"")
})

public class PlayerAgent extends MicroAgent {
	
	// Logging support
    private static Logger logger = Logger.getLogger (ProjectLogger.class.getName ());

	protected static String playerPath="/usr/local/bin/player";
	protected OSCommand startPlayer = null;
	protected static int port = 6665;

	public IFuture agentCreated()
	{
//		ProjectLogger.logActivity(false, "running", this.toString(), port, Thread.currentThread().getName());
		logger.info("Running "+getComponentIdentifier().toString());
		
		port = ((Integer)getArgument("player port")).intValue();

		if ((Boolean)getArgument("requires player") == true) {
			// Get the Gui argument, if any
			String[] command = {
					((String)getArgument("player path")),
					new String("-p ").concat( String.valueOf(port) ),
					((String)getArgument("player config"))
			};

			startPlayer = new OSCommand(command);
		}
		return IFuture.DONE;
	}

//	protected void agentStarted () {};

	public IFuture executeBody()
	{	
		// TODO no blocking
//		agentBody();
//		if (startPlayer != null) {
//			startPlayer.waitFor();
//		}
		return new Future();
	}
	
	public IFuture agentKilled()
	{
		if (startPlayer != null) {
			startPlayer.terminate();
		}
//		ProjectLogger.logActivity(false, "Termination", this.toString(), port, Thread.currentThread().getName());
		logger.info("Termination "+getComponentIdentifier().toString());
		return IFuture.DONE;
	}
	
//	public static MicroAgentMetaInfo getMetaInfo()
//	{
//		IArgument[] args = {
//				new Argument("requires player", "dummy", "Boolean", new Boolean(false)),
//				new Argument("player path", "dummy", "String", playerPath),
//				new Argument("player port", "dummy", "Integer", new Integer(port)),	
//				new Argument("player config", "dummy", "String", "/Users/sebastian/robotcolla/SimpleAgent/player/uhh1.cfg")};
//		
//		return new MicroAgentMetaInfo("This agent starts up a Player agent.", null, args, null);
//	}
}
