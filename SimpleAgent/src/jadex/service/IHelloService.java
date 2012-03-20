package jadex.service;



/**
 *  Service can receive chat messages.
 */
public interface IHelloService
{
	/**
	 *  Hear something.
	 *  @param name The name of the sender.
	 *  @param robotName The name of the robot.
	 *  @param content The message object (normally a @see String).
	 */
	public void receive(String name, String robotName, String content);
		
}
