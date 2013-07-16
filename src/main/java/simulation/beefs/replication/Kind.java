package simulation.beefs.replication;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.Machine.State;
import simulation.beefs.model.ReplicatedFile;

/**
 * 
 * Keep machines sleeping if some other machine can get the replica
 *
 */
public class Kind extends Replicator {
	
	private final Set<DataServer> dataServers;
	
	public Kind(Set<DataServer> dataServers) {
		this.dataServers = dataServers;
	}

	@Override
	public ReplicatedFile updateReplicas(ReplicatedFile file) {
		if(!file.getPrimary().getHost().isReachable()) {
			file.getPrimary().getHost().wakeOnLan(EventScheduler.now());
		}
		Set<DataServer> newDataServers = new HashSet<DataServer>();
		for(DataServer ds : file.getSecondaries()) {
			if(ds.getHost().getState().equals(State.SLEEPING)) {
				newDataServers.add(giveMeOneAwakeDataServer(newDataServers));
			} else {
				newDataServers.add(ds);
			}
		}
		return new ReplicatedFile(file.getFullPath(), file.getPrimary(), newDataServers);
	}

	private DataServer giveMeOneAwakeDataServer(Set<DataServer> exceptions) {
		for(DataServer ds : dataServers) {
			if(ds.getHost().isReachable() && !exceptions.contains(ds)) {
				return ds;
			}
		}
		return wakeUpWhoIsSleepingForLonger();
	}

	private DataServer wakeUpWhoIsSleepingForLonger() {
		DataServer unfortunateDataServer = null;
		
		for(DataServer ds : dataServers) {
			if(!ds.getHost().isReachable()) {
				if(unfortunateDataServer == null) {
					unfortunateDataServer = ds;
				} else if(ds.getHost().lastTransitionTime().
						isEarlierThan(unfortunateDataServer.getHost().lastTransitionTime())) {
					unfortunateDataServer = ds;
				}
			}
		}
		
		unfortunateDataServer.getHost().wakeOnLan(EventScheduler.now());
		return unfortunateDataServer;
	}

}
