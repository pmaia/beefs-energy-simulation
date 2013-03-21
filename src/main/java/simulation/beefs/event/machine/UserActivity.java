package simulation.beefs.event.machine;

import manelsim.Time;
import simulation.beefs.event.MachineDelaybleEvent;
import simulation.beefs.model.Machine;

/**
 *
 * @author Patrick Maia
 */
public class UserActivity extends MachineDelaybleEvent {
	
	private final Machine host;
	private final Time duration;
	
	public UserActivity(Machine host, Time scheduledTime, Time duration) {
		this(host, scheduledTime, duration, true);
	}
	
	public UserActivity(Machine host, Time scheduledTime, Time duration, boolean delayable) {
		super(host, scheduledTime, delayable);
		
		this.host = host;
		this.duration = duration;
	}
	
	@Override
	public void process() {
		host.setActive(getScheduledTime(), duration);
	}

	@Override
	public String toString() {
		return String.format("activity\t%s\t%s\t%s", getScheduledTime(), duration, host.getName());
	}
	
	public Time getDuration() {
		return this.duration;
	}
}
