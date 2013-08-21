package simulation.beefs.replication;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileReplica;
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
	public void updateReplicas(ReplicatedFile file) {
		if(!file.primary().getHost().isReachable()) {
			file.primary().getHost().wakeOnLan(EventScheduler.now());
		}
		
		Set<DataServer> exceptions = new HashSet<DataServer>();
		Set<FileReplica> newReplicas = new HashSet<FileReplica>();
		
		for(FileReplica replica : file.replicas()) {
			exceptions.add(replica.dataServer());
			replica.delete();
			
			DataServer newDataServer = null;
			if(replica.dataServer().getHost().getState().equals(State.SLEEPING)) {
				newDataServer = giveMeOneAwakeDataServer(exceptions);
				exceptions.add(newDataServer);
			} else {
				newDataServer = replica.dataServer();
			}
			
			newReplicas.add(new FileReplica(newDataServer, file.size()));
		}
		file.updateReplicas(newReplicas);
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
