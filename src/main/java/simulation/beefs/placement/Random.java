package simulation.beefs.placement;

import static simulation.beefs.placement.DataPlacementUtil.chooseRandomDataServers;

import java.util.HashSet;
import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.ReplicatedFile;

public class Random extends DataPlacement {
	
	public Random(Set<DataServer> dataServers) {
		super(dataServers);
	}

	@Override
	public ReplicatedFile createFile(FileSystemClient client, String fileName, int replicationLevel) {
		
		Set<DataServer> choosenDataServes = 
			chooseRandomDataServers(dataServers, replicationLevel + 1);
		
		DataServer primary = null;
		Set<DataServer> secondaries = new HashSet<DataServer>();
		
		for(DataServer dataServer : choosenDataServes) {
			if(primary == null) {
				primary = dataServer;
			} else {
				secondaries.add(dataServer);
			}
		}
		
		return new ReplicatedFile(fileName, primary, secondaries);
		
	}

}