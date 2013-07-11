package simulation.beefs.replication;

import simulation.beefs.model.ReplicatedFile;

public interface Replicator {
	ReplicatedFile updateReplicas(ReplicatedFile file);
}
