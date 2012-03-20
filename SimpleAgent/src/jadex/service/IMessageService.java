package jadex.service;



/**
 *  Service can receive chat messages.
 */
public interface IMessageService 
{
	/**
	 *  Hear something.
	 *  @param name The name of the sender.
	 *  @param text The text message.
	 */
	public void hear(String name, String text);
		
}
