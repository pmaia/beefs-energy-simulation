package simulation.beefs.energy;

import manelsim.TimeInterval;

public class EnergyStateInterval {
	private final EnergyState state;
	private final TimeInterval interval;
	
	public EnergyStateInterval(EnergyState state, TimeInterval interval) {
		this.state = state;
		this.interval = interval;
	}
	
	public EnergyState getEnergyState() {
		return state;
	}
	
	public TimeInterval getInterval() {
		return interval;
	}
	
	@Override
	public String toString() {
		return String.format("<%s - %s>", state, interval);
	}
}