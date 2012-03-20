package jadex;


import jadex.bridge.modelinfo.IArgument;
import jadex.micro.MicroAgentMetaInfo;
import jadex.micro.annotation.Argument;
import jadex.micro.annotation.Arguments;


/**
 * Generic agent class for PlayerStage requirements
 * @author sebastian
 *
 */
@Arguments({
	@Argument(name="requires player", description="dummy", clazz=Boolean.class, defaultvalue="true" ),
	@Argument(name="player path", description="dummy", clazz=String.class, defaultvalue="playerPath"),
	@Argument(name="player port", description="dummy", clazz=Integer.class, defaultvalue="port"),
	@Argument(name="player config", description="dummy", clazz=String.class, defaultvalue="\"/Users/sebastian/robotcolla/SimpleAgent/player/uhh1.cfg\"")
})
public class SimulationAgent extends PlayerAgent {
	
//	public static MicroAgentMetaInfo getMetaInfo()
//	{
//		IArgument[] args = {
//				new Argument("requires player", "dummy", "Boolean", new Boolean(true)),
//				new Argument("player path", "dummy", "String", playerPath),
//				new Argument("player port", "dummy", "Integer", new Integer(port)),	
//				new Argument("player config", "dummy", "String", "/Users/sebastian/robotcolla/SimpleAgent/player/uhh1.cfg")};
//		
//		return new MicroAgentMetaInfo("This agent starts up a Player agent.", null, args, null);
//	}
}
