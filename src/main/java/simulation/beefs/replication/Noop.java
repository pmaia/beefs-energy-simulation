package simulation.beefs.replication;

import simulation.beefs.model.ReplicatedFile;

public class Noop extends Replicator {

	@Override
	public void updateReplicas(ReplicatedFile file) { /* empty */ }

}
