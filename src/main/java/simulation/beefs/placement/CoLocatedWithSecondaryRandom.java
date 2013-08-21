package simulation.beefs.placement;

import static simulation.beefs.placement.DataPlacementUtil.chooseRandomDataServers;

import java.util.HashSet;
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
	public ReplicatedFile createFile(FileSystemClient client, String fileName, int replicationLevel) {
		
		DataServer primary = null;
		Set<DataServer> secondaries;
		
		DataServer colocatedDataServer = 
			client.getMetadataServer().getDataServer(client.getHost().getName());
		
		if(colocatedDataServer != null) {
			Set<DataServer> copyOfAvailableDataServers = 
				new HashSet<DataServer>(dataServers);
			
			copyOfAvailableDataServers.remove(colocatedDataServer);

			primary = colocatedDataServer;
			
			secondaries = 
				chooseRandomDataServers(copyOfAvailableDataServers, replicationLevel);
			
		} else {
			
			secondaries = new HashSet<DataServer>();
			
			for(DataServer dataServer : chooseRandomDataServers(dataServers, replicationLevel)) {
				if(primary == null) {
					primary = dataServer;
				} else {
					secondaries.add(dataServer);
				}
			}
			
		}
		
		Set<FileReplica> replicas = new HashSet<FileReplica>();
		for(DataServer ds : secondaries) {
			replicas.add(new FileReplica(ds, 0));
		}

		return new ReplicatedFile(fileName, primary, replicationLevel, replicas);
	}

}
