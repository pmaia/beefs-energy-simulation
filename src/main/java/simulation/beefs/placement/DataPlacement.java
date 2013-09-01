package simulation.beefs.placement;

import java.util.Set;

import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.ReplicatedFile;

public abstract class DataPlacement {
	
	public static final String RANDOM = "random";
	public static final String CO_RANDOM = "co-random";
	
	protected Set<DataServer> dataServers;
	
	public DataPlacement(Set<DataServer> dataServers) {
		this.dataServers = dataServers;
	}
	
	public static DataPlacement newDataPlacement(String type, Set<DataServer> dataServers) {
		if(CO_RANDOM.equals(type)) {
			return new CoLocatedWithSecondaryRandom(dataServers);
		} else if(RANDOM.equals(type)) {
			return new Random(dataServers);
		} else {
			throw new IllegalArgumentException(type + " is not a valid DataPlacementAlgorithm type.");
		}
	}

	public abstract ReplicatedFile createFile(FileSystemClient client, String fullpath, int replicationLevel, long size);
	
}
