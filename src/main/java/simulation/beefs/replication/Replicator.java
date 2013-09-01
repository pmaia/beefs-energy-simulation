package simulation.beefs.replication;

import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.ReplicatedFile;

public abstract class Replicator {
	
	private static final String KIND =  "kind";
	private static final String NOOP = "noop";
	private static final String FAITHFUL = "faithful";
	
	public static Replicator newReplicator(String type, Set<DataServer> dataServers) {
		Replicator replicator = null;
		if(KIND.equals(type)) {
			replicator = new MigrateReplicas(dataServers);
		} else if(NOOP.equals(type)) {
			replicator = new Noop();
		} else if(FAITHFUL.equals(type)) {
			replicator = new Faithful();
		} else {
			throw new RuntimeException("Unknown replicator type " + type);
		}
		return replicator;
	}
	
	public abstract void updateReplicas(ReplicatedFile file);
}
