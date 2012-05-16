package jadex;

import jadex.commons.future.IFuture;
import jadex.commons.future.Future;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;

import java.util.logging.Logger;

import core.OSCommand;

@Arguments({
	@Argument(name="killall path",clazz=String.class, defaultvalue="\"/usr/bin/killall\""),
	@Argument(name="process name",clazz=String.class,defaultvalue="\"player\"")})

@SuppressWarnings({"rawtypes", "unchecked"})
public class KillPlayerAgent extends MicroAgent {
	
	// Logging support
    private static Logger logger = Logger.getLogger (KillPlayerAgent.class.getName ());

	protected OSCommand stopPlayer = null;

	public IFuture agentCreated()
	{
		logger.info("Running "+getComponentIdentifier().toString());

		// Get the Gui argument, if any
		String[] command = {
				(String)getArgument("killall path"),
				(String)getArgument("process name")
		};
		stopPlayer = new OSCommand(command);
		return IFuture.DONE;
	}

	public IFuture executeBody() {
		stopPlayer.waitFor();
		//killAgent();
		return new Future();
	}
	
	public IFuture agentKilled()
	{		
		stopPlayer.terminate();
//		ProjectLogger.logActivity(false, "Termination", this.toString(), -1, Thread.currentThread().getName());
		logger.info("Termination "+getComponentIdentifier().toString());
		return IFuture.DONE;
	}
	
//	public static MicroAgentMetaInfo getMetaInfo()
////	{
////		Argument[] args = {
////				new Argument("killall path", "dummy", "String", "/usr/bin/killall"),
//				new Argument("process name", "dummy", "String", "player")};
////		
//		return new MicroAgentMetaInfo("This agent kills all 'player' instances on this host and exits.", null, args, null);
//	}
}