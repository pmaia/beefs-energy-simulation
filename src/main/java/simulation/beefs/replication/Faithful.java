package simulation.beefs.replication;

import manelsim.EventScheduler;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.Machine;
import simulation.beefs.model.ReplicatedFile;

/**
 * 
 * All replicas are bind to a specific data server
 *
 */
public class Faithful implements Replicator {

	@Override
	public ReplicatedFile updateReplicas(ReplicatedFile file) {
		wakeUpIfSleeping(file.getPrimary().getHost());
		for(DataServer replicaDataServer : file.getSecondaries()) {
			wakeUpIfSleeping(replicaDataServer.getHost());
		}
		return file;
	}
	
	private void wakeUpIfSleeping(Machine machine) {
		if(!machine.isReachable()) {
			machine.wakeOnLan(EventScheduler.now());
		}
	}

}
