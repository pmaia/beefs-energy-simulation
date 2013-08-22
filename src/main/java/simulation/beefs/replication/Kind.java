package simulation.beefs.replication;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileReplica;
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

		DataServer newDataServer = null;
		for(FileReplica replica : file.replicas()) {
			exceptions.add(replica.dataServer());
			replica.delete();
			
			if(replica.dataServer().getHost().isReachable() && replica.dataServer().freeSpace() > file.size()) {
				newDataServer = replica.dataServer();
			} else {
				newDataServer = giveMeOneAwakeDataServer(exceptions, file.size());
			}
			
			if(newDataServer != null) {
				exceptions.add(newDataServer);
				newReplicas.add(new FileReplica(newDataServer, file.size()));
			} else {
				break;
			}
		}
		
		if(newReplicas.size() < file.expectedReplicationLevel() && newDataServer != null) {
			for(int i = 0; i < (file.expectedReplicationLevel() - newReplicas.size()); i++) {
				newDataServer = giveMeOneAwakeDataServer(exceptions, file.size());

				if(newDataServer != null) {
					exceptions.add(newDataServer);
					newReplicas.add(new FileReplica(newDataServer, file.size()));
				} else {
					break;
				}
			}
		}
		
		file.updateReplicas(newReplicas);
	}

	private DataServer giveMeOneAwakeDataServer(Set<DataServer> exceptions, long fileSize) {
		for(DataServer ds : dataServers) {
			if(ds.getHost().isReachable() && !exceptions.contains(ds) && ds.freeSpace() >= fileSize) {
				return ds;
			}
		}
		return wakeUpWhoIsSleepingForLonger(fileSize);
	}

	private DataServer wakeUpWhoIsSleepingForLonger(long fileSize) {
		DataServer unfortunateDataServer = null;
		
		for(DataServer ds : dataServers) {
			if(!ds.getHost().isReachable()) {
				if(unfortunateDataServer == null && ds.freeSpace() >= fileSize) {
					unfortunateDataServer = ds;
				} else if(ds.getHost().lastTransitionTime().
						isEarlierThan(unfortunateDataServer.getHost().lastTransitionTime()) && ds.freeSpace() >= fileSize) {
					unfortunateDataServer = ds;
				}
			}
		}
		if( unfortunateDataServer != null) {
			unfortunateDataServer.getHost().wakeOnLan(EventScheduler.now());
		} else {
			System.out.println(String.format("@all disks full: could not replicate %d bytes - %s", fileSize, EventScheduler.now()));
		}
		return unfortunateDataServer;
	}

}
