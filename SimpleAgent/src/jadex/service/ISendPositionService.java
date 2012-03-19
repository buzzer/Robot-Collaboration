package jadex.service;

import jadex.bridge.service.IService;
import data.Position;


/**
 *  Service can receive chat messages.
 */
public interface ISendPositionService extends IService
{
	/**
	 *  Hear something.
	 *  @param name The name of the sender.
	 *  @param robotName The robot name.
	 *  @param content The @see Position.
	 */
	public void receive(String name, String robotName, Object content);
	public void send(String name, String robotName, Position content);
	
}
