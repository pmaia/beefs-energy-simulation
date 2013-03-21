package simulation.beefs.event.machine;

import manelsim.Event;
import manelsim.Time;
import simulation.beefs.model.Machine;

/**
 * @author Patrick Maia
 */
public class WakeOnLan extends Event {
	
	private final Machine machine;

	public WakeOnLan(Machine machine, Time scheduledTime) {
		super(scheduledTime);
		this.machine = machine;
	}

	@Override
	public void process() {
		machine.wakeOnLan(getScheduledTime());
	}
	
	@Override
	public String toString() {
		return String.format("wakeOnLan\t%s\t%s", getScheduledTime(), machine.getName());
	}

}
