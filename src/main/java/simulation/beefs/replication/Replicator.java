package simulation.beefs.replication;

import simulation.beefs.model.ReplicatedFile;

public interface Replicator {
	void updateReplicas(ReplicatedFile file);
}
