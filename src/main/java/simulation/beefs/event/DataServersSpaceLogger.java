package simulation.beefs.event;

import java.util.Set;

import manelsim.EventScheduler;
import manelsim.RepeatableEvent;
import manelsim.Time;
import simulation.beefs.model.DataServer;

public class DataServersSpaceLogger extends RepeatableEvent {
	
	private final Set<DataServer> dataServers;

	public DataServersSpaceLogger(Time firstTime, Time interval, Set<DataServer> dataServers) {
		super(firstTime, interval);
		this.dataServers = dataServers;
	}

	@Override
	public void work() {
		for(DataServer ds : dataServers) {
			System.out.println(String.format("#%s\t%s\t%d", EventScheduler.now(), ds.getHost().getName(), ds.freeSpace()));
		}
	}

}
