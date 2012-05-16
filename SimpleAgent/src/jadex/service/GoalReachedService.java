package jadex.service;

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
 * This service sends new goals to agents on the network.
 * No sending is required of participating agents.
 * @author sebastian
 *
 */
@Service
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GoalReachedService implements IGoalReachedService {

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
	public GoalReachedService()
	{
		//super(agent.getServiceProvider().getId(), IGoalReachedService.class, null);
		this.agent = (IMicroExternalAccess)agent;
		this.listeners = Collections.synchronizedList(new ArrayList());
	}
	
	//-------- methods --------	
	/**
	 *  Tell something.
	 *  @param name The name.
	 *  @param robotName The text.
	 *  @param obj The goal reached.
	 */
	public void send(final String name, final String robotName, final Object obj)
	{
		SServiceProvider.getServices(agent.getServiceProvider(), IGoalReachedService.class,RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object result)
			{
				if(result!=null)
				{
					for(Iterator it=((Collection)result).iterator(); it.hasNext(); )
					{
						IGoalReachedService gr = (IGoalReachedService)it.next();
						gr.receive(name, robotName, obj);
					}
				}
			}
		});
	}
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param obj The text.
	 */
	public void receive(String name, String robotName, Object obj)
	{
		IChangeListener[] lis = (IChangeListener[])listeners.toArray(new IChangeListener[0]);
		for(int i=0; i<lis.length; i++)
		{
			lis[i].changeOccurred(new ChangeEvent(this, null, new Object[]{name, robotName, obj}));
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
		return "TestService, "+agent.getComponentIdentifier();
	}
}
