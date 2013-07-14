package simulation.beefs.event.machine;

import manelsim.Event;
import manelsim.Time;
import simulation.beefs.model.Machine;

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

}
