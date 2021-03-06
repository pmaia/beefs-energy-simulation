package simulation.beefs.event.machine;

import manelsim.Time;
import simulation.beefs.event.MachineDelaybleEvent;
import simulation.beefs.model.Machine;

public class UserIdleness extends MachineDelaybleEvent {
	
	private final Machine machine;
	
	private final Time duration;
	
	public UserIdleness(Machine machine, Time scheduledTime, Time duration) {
		this(machine, scheduledTime, duration, true);
	}
	
	public UserIdleness(Machine machine, Time scheduledTime, Time duration, boolean delayable) {
		super(machine, scheduledTime, delayable);
		
		this.machine = machine;
		this.duration = duration;
	}

	@Override
	public void process() {
		machine.setIdle(getScheduledTime(), duration);
	}
	
	public Time getDuration() {
		return this.duration;
	}

}
