package simulation.beefs.replication;

import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.ReplicatedFile;

public abstract class Replicator {
	
	private static final String MIGRATE_REPLICAS_WAKE_UP =  "migrate_replicas_wake_up";
	private static final String MIGRATE_REPLICAS_NEVER_WAKE_UP =  "migrate_replicas_never_wake_up";
	private static final String NOOP = "noop";
	private static final String NEVER_MIGRATE_REPLICAS = "never_migrate_replicas";
	
	public static Replicator newReplicator(String type, Set<DataServer> dataServers) {
		Replicator replicator = null;
		if(MIGRATE_REPLICAS_WAKE_UP.equals(type)) {
			replicator = new MigrateReplicas(dataServers, true);
		} else if(MIGRATE_REPLICAS_NEVER_WAKE_UP.equals(type)) {
			replicator = new MigrateReplicas(dataServers, false);
		} else if(NEVER_MIGRATE_REPLICAS.equals(type)) {
			replicator = new NeverMigrateReplicas();
		} else if(NOOP.equals(type)) {
			replicator = new Noop();
		} else {
			throw new RuntimeException("Unknown replicator type " + type);
		}
		return replicator;
	}
	
	public abstract void updateReplicas(ReplicatedFile file);
}
