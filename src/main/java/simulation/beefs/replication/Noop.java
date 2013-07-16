package simulation.beefs.replication;

import simulation.beefs.model.ReplicatedFile;

public class Noop extends Replicator {

	@Override
	public ReplicatedFile updateReplicas(ReplicatedFile file) {
		return file;
	}

}
