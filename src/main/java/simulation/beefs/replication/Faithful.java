package simulation.beefs.replication;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import simulation.beefs.model.FileReplica;
import simulation.beefs.model.Machine;
import simulation.beefs.model.ReplicatedFile;

/**
 * 
 * All replicas are bind to a specific data server
 *
 */
public class Faithful extends Replicator {

	@Override
	public void updateReplicas(ReplicatedFile file) {
		Set<FileReplica> newReplicas = new HashSet<FileReplica>();
		wakeUpIfSleeping(file.primary().getHost());
		for(FileReplica replica : file.replicas()) {
			replica.delete();
			wakeUpIfSleeping(replica.dataServer().getHost());
			newReplicas.add(new FileReplica(replica.dataServer(), file.size()));
		}
		file.updateReplicas(newReplicas);
	}
	
	private void wakeUpIfSleeping(Machine machine) {
		if(!machine.isReachable()) {
			machine.wakeOnLan(EventScheduler.now());
		}
	}

}
