package jadex.service;

import jadex.bridge.IExternalAccess;
import jadex.bridge.service.BasicService;
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
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Service
public class MessageService implements IMessageService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IMicroExternalAccess agent;
	
	/** The listeners. */
	protected List listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new helpline service.
	 */
	public MessageService()
	{
		//super(agent.getServiceProvider().getId(), IMessageService.class, null);
		this.agent = (IMicroExternalAccess)agent;
		this.listeners = Collections.synchronizedList(new ArrayList());
	}
	
	//-------- methods --------
	
	/**
	 *  Tell something.
	 *  @param name The name.
	 *  @param text The text.
	 */
	public void tell(final String name, final String text)
	{
		SServiceProvider.getServices(agent.getServiceProvider(), IMessageService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DefaultResultListener()
		{

			public void resultAvailable(Object result)
			{
				if(result!=null)
				{
					for(Iterator it=((Collection)result).iterator(); it.hasNext(); )
					{
						IMessageService cs = (IMessageService)it.next();
						cs.hear(name, text);
					}
				}
			}

		});
	}
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param text The text.
	 */
	public void hear(String name, String text)
	{
		IChangeListener[] lis = (IChangeListener[])listeners.toArray(new IChangeListener[0]);
		for(int i=0; i<lis.length; i++)
		{
			lis[i].changeOccurred(new ChangeEvent(this, null, new String[]{name, text}));
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
		return "ChatService, "+agent.getComponentIdentifier();
	}
}
