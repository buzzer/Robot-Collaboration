package jadex.service;

import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.DefaultResultListener;
import jadex.micro.IMicroExternalAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements a Hello service.
 * Every agent on the network is required to send a hello message
 * on this service when it is started.
 * A normal agent is not required to receive any message on this service.
 * A control agent will receive this message (if it is started)
 * and then knows that this agent is available.
 * @author sebastian
 *
 */
@Service
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HelloService implements IHelloService {

//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IMicroExternalAccess agent;
	
	/** The listeners. */
	protected List<IChangeListener> listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new helpline service.
	 */
	public HelloService()
	{
		//super(agent.getServiceProvider().getId(), IHelloService.class, null);
		this.agent = (IMicroExternalAccess)agent;
		this.listeners = Collections.synchronizedList(new ArrayList());
	}
	
	//-------- methods --------	
	/**
	 *  Tell something.
	 *  @param name The name.
	 *  @param robotName The text.
	 *  @param obj
	 */
//	public void send(final String name, final String robotName, final String obj)
	public static void send(final String name, final String robotName, final String obj, IExternalAccess agent)
	{
		SServiceProvider.getServices(agent.getServiceProvider(), IHelloService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				if(result!=null)
				{
					for(Iterator it=((Collection)result).iterator(); it.hasNext(); )
					{
						IHelloService hs = (IHelloService)it.next();
						hs.receive(name, robotName, obj);
					}
				}
			}
		});
	}
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param robotName The text.
	 *  @param content The string content.
	 */
	public void receive(String name, String robotName, String content)
	{
		IChangeListener[] lis = (IChangeListener[])listeners.toArray(new IChangeListener[0]);
		for(int i=0; i<lis.length; i++)
		{
			lis[i].changeOccurred(new ChangeEvent(this, null, new Object[]{name, robotName, content}));
		}
	}
	
	/**
	 *  Add a change listener.
	 */
	public void addChangeListener(IChangeListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 *  Remove a change listener.
	 */
	public void removeChangeListener(IChangeListener listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "HelloService, "+agent.getComponentIdentifier();
	}
}
