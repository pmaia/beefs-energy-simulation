package simulation.beefs.event.machine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import manelsim.Event;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.Time;
import manelsim.Time.Unit;
import simulation.beefs.model.Machine;


/**
 * 
 * A parser for traces of machine activity.
 * 
 * This parser expects that activity is logged in the format below:
 * <br><br>
 * &lt;idleness&gt;\t&lt;start_timestamp&gt;\t&lt;duration&gt;
 * <br><br>
 * where &lt;start_timestamp&gt; are the seconds since epoch in which the event 
 * started and &lt;duration&gt; is the time in seconds during which the event lasted.
 *
 * @author Patrick Maia - patrickjem@lsd.ufcg.edu.br
 */
public class UserActivityTraceEventSource implements EventSource {
	
	private final Machine machine;
	private final BufferedReader eventReader;
	private Event firstEvent;
	
	public UserActivityTraceEventSource(Machine machine, InputStream eventStream) {
		this.machine = machine;
		this.eventReader = new BufferedReader(new InputStreamReader(eventStream));
		this.firstEvent = advanceToSimulationStart();
	}

	@Override
	public Event getNextEvent() {
		Event event = null;
		
		if(firstEvent != null) {
			event = firstEvent;
			firstEvent = null;
		} else {
			try {
				String traceLine = eventReader.readLine();
				
				if(traceLine != null) {
					String [] tokens = traceLine.split("\\s");
					
					if(tokens.length != 3) {
						throw new RuntimeException("Bad formatted line: " + traceLine);
					}
					
					String eventType = tokens[0];
					Time aScheduledTime = new Time(Long.parseLong(tokens[1]), Unit.SECONDS);
					Time duration = new Time(Long.parseLong(tokens[2]), Unit.SECONDS);
					
					if(eventType.equals("idleness")) {
						event = new UserIdleness(machine, aScheduledTime, duration);
					} else if(eventType.equals("activity")) {
						event = new UserActivity(machine, aScheduledTime, duration);
					} else {
						throw new RuntimeException(eventType + " is not recognized by this parser as a valid event type.");
					}
				}
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return event;
	}

	/**
	 * Advances this EventSource to the interval that contains the simulation start time. That interval start becomes
	 * the simulation start time and the duration is adjusted accordingly
	 */
	private Event advanceToSimulationStart() {
		Time emulationStartTime = EventScheduler.getEmulationStart();
		
		Event event;
		Time duration;
		do {
			event = getNextEvent();
			duration = getDuration(event);
		} while(event.getScheduledTime().plus(duration).isEarlierThan(emulationStartTime));
		
		Time newDuration = event.getScheduledTime().plus(duration).minus(emulationStartTime);
		
		if(event instanceof UserActivity) {
			return new UserActivity(machine, emulationStartTime, newDuration);
		} else {
			return new UserIdleness(machine, emulationStartTime, newDuration);
		}
	}
	
	private Time getDuration(Event event) {
		try {
			Method getDuration = event.getClass().getMethod("getDuration");
			return (Time)getDuration.invoke(event);
		} catch (Throwable t) {
			throw new RuntimeException("Surprise!", t);
		}
	}
}
