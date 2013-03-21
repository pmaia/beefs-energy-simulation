package simulation.beefs.event.machine;

import manelsim.Event;
import manelsim.Time;
import simulation.beefs.model.Machine;

/**
 *
 * @author Patrick Maia
 */
public class Sleep extends Event {
	
	private final Machine machine; 
	
	private final Time duration;
	
	public Sleep(Machine machine, Time scheduledTime, Time duration) {
		super(scheduledTime);
		
		this.machine = machine;
		this.duration = duration;
	}
	
	@Override
	public void process() {
		machine.setSleeping(getScheduledTime(), duration);
	}
	
	@Override
	public String toString() {
		return String.format("sleep\t%s\t%s\t%s", getScheduledTime(), duration, machine.getName());
	}
}
