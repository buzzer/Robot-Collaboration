package jadex.service;

import jadex.bridge.IExternalAccess;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.IFuture;
import jadex.commons.future.DefaultResultListener;
import jadex.bridge.service.BasicService;
import jadex.micro.IMicroExternalAccess;
import jadex.micro.annotation.Binding;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.annotation.*;
import jadex.bridge.service.search.SServiceProvider;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This service sends new goals to agents on the network.
 * No sending is required of participating agents.
 * @author sebastian
 *
 */
@Service
public class GoalReachedService extends BasicService implements IGoalReachedService
//public class GoalReachedService implements IGoalReachedService
{
//-------- attributes --------
	
	/** The agent. */
	protected IMicroExternalAccess agent;
	
	/** The listeners. */
	@SuppressWarnings("rawtypes")
	protected List listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new helpline service.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GoalReachedService(IExternalAccess agent)
	{
		super(agent.getServiceProvider().getId(), IGoalReachedService.class, null);
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
	public IFuture<Void> send(final String name, final String robotName, final Object obj)
	{
		SServiceProvider.getServices(agent.getServiceProvider(), IGoalReachedService.class, Binding.SCOPE_GLOBAL)
			.addResultListener(new DefaultResultListener()
		{
			@Override
			@SuppressWarnings("rawtypes")
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
		return IFuture.DONE;
	}
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param obj The text.
	 */
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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

	@Override
	public IServiceIdentifier getServiceIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFuture<Boolean> isValid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getPropertyMap() {
		// TODO Auto-generated method stub
		return null;
	}
}
