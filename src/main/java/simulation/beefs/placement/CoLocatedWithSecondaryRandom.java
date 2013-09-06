package simulation.beefs.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileReplica;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.ReplicatedFile;

public class CoLocatedWithSecondaryRandom extends DataPlacement {

	public CoLocatedWithSecondaryRandom(Set<DataServer> dataServers) {
		super(dataServers);
	}

	@Override
	public ReplicatedFile createFile(FileSystemClient client, String fileName, int replicationLevel, long size) {

		DataServer primary = null;
		List<DataServer> availableDataServers = new ArrayList<DataServer>(dataServers);

		DataServer colocatedDataServer = client.metadataServer().getDataServer(client.host().name());
		availableDataServers.remove(colocatedDataServer);

		Collections.shuffle(availableDataServers);

		if(colocatedDataServer != null && colocatedDataServer.freeSpace() >= size) {
			primary = colocatedDataServer;
		} 

		Iterator<DataServer> dsIterator = availableDataServers.iterator();
		while(primary == null && dsIterator.hasNext()) {
			DataServer dsCandidate = dsIterator.next();
			if(dsCandidate.freeSpace() >= size) {
				primary = dsCandidate;
				availableDataServers.remove(dsCandidate);
			}
		}

		ReplicatedFile rf = null;
		if(primary != null) {
			Set<FileReplica> replicas = createReplicas(availableDataServers, replicationLevel);
			return new ReplicatedFile(fileName, primary, replicationLevel, replicas);
		}
		
		return rf;
	}

	private Set<FileReplica> createReplicas(List<DataServer> availableDataServers, int replicationLevel) {
		Set<FileReplica> replicas = new HashSet<FileReplica>();

		Iterator<DataServer> dsIterator = availableDataServers.iterator();
		while(dsIterator.hasNext() && replicas.size() < replicationLevel) {
			replicas.add(new FileReplica(dsIterator.next(), 0));
		}

		return replicas;
	}

}
