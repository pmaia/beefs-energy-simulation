/* JEEventHandler - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package ddg.kernel;


/**
 * TODO make doc
 * 
 * @author thiago - thiago@lsd.ufcg.edu.br
 */
public abstract class EventHandler {

	private static Integer theUniqueHandlerId;
	private final Integer HandlerId;
	private final EventScheduler theUniqueEventScheduler;

	/**
	 * @param scheduler
	 */
	public EventHandler(EventScheduler scheduler) {

		if (theUniqueHandlerId != null) {
			theUniqueHandlerId = Integer.valueOf(theUniqueHandlerId.intValue() + 1);
		} else {
			theUniqueHandlerId = Integer.valueOf(1);
		}
		HandlerId = theUniqueHandlerId;
		theUniqueEventScheduler = scheduler;
		theUniqueEventScheduler.registerHandler(this);
	}

	/**
	 * @return
	 */
	protected EventScheduler getScheduler() {
		return theUniqueEventScheduler;
	}

	/**
	 * @param jeevent
	 */
	public abstract void handleEvent(Event jeevent);

	/**
	 * @param anEvent
	 */
	public void send(Event anEvent) {
		theUniqueEventScheduler.schedule(anEvent);
	}

	/**
	 * @return
	 */
	public Integer getHandlerId() {
		return HandlerId;
	}
}
