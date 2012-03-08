package jadex.service;

import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.IMicroExternalAccess;

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
public class ReceiveNewGoalService extends BasicService implements IReceiveNewGoalService
{

//-------- attributes --------
	

	

	@ServiceComponent
	IExternalAccess agent;
	
	/** The listeners. */
	@SuppressWarnings("rawtypes")
	protected List listeners;
	
	//-------- constructors --------
	
	/**
	 *  Create a new helpline service.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ReceiveNewGoalService(IExternalAccess agent)
	{
		super(agent.getServiceProvider().getId(), IReceiveNewGoalService.class, null);
		this.agent = (IMicroExternalAccess)agent;
		this.listeners = Collections.synchronizedList(new ArrayList());
	}
	
	//-------- methods --------	
	/**
	 *  Tell something.
	 *  @param name The name.
	 *  @param robotName The robot name.
	 *  @param obj A new goal.
	 */
	public static void send(IExternalAccess agent, final String name, final String robotName, final Object obj)
	{
		
		SServiceProvider.getServices(agent.getServiceProvider(), IReceiveNewGoalService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new DefaultResultListener()
		{
			@SuppressWarnings("rawtypes")
			
			public void resultAvailable(Object result)
			{
				if(result!=null)
				{
					for(Iterator it=((Collection)result).iterator(); it.hasNext(); )
					{
						IReceiveNewGoalService ts = (IReceiveNewGoalService)it.next();
						ts.receive(name, robotName, obj);
					}
				}
			}
		});
	}
	
	/**
	 *  Hear something.
	 *  @param name The name.
	 *  @param robotName The robot name.
	 *  @param obj The new goal.
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
