package simulation.beefs.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import manelsim.EventScheduler;
import manelsim.Time;
import simulation.beefs.event.filesystem.UpdateFileReplicas;
import simulation.beefs.placement.DataPlacement;
import simulation.beefs.replication.Replicator;

public class MetadataServer {
	
	private final DataPlacement dataPlacement;
	
	private final Replicator replicator;
	
	private final int replicationLevel;
	
	private final Time timeToCoherence;
	
	private final Map<String, ReplicatedFile> files = new HashMap<String, ReplicatedFile>();
	
	private final Map<String, UpdateFileReplicas> scheduledUpdateReplicasEvents = new HashMap<String, UpdateFileReplicas>();

	// Patrick: I'm considering that there is just one DataServer per machine.
	private final Map<String, DataServer> dataServerByHost = new HashMap<String, DataServer>();

	public MetadataServer(Set<DataServer> dataServers, DataPlacement dataPlacementStrategy, 
			Replicator replicator, int replicationLevel, Time timeToCoherence) {
		
		for(DataServer dataServer : dataServers) {
			dataServerByHost.put(dataServer.host().name(), dataServer);
		}
		
		this.dataPlacement = dataPlacementStrategy;
		this.replicator = replicator;
		this.replicationLevel = replicationLevel;
		this.timeToCoherence = timeToCoherence;
	}
	
	public void close(String filePath) {
		ReplicatedFile file = files.get(filePath);
		
		if(file != null && !file.areReplicasConsistent() && file.replicas().size() > 0) {
			cleanUpProcessedEvents();
			Time now = EventScheduler.now();
			UpdateFileReplicas old = scheduledUpdateReplicasEvents.get(file.fullPath());
			if(old != null) {
				EventScheduler.cancel(old);
			}
			UpdateFileReplicas updateFileReplicas = new UpdateFileReplicas(now.plus(timeToCoherence), file, this);
			EventScheduler.schedule(updateFileReplicas);
			scheduledUpdateReplicasEvents.put(file.fullPath(), updateFileReplicas);
		}
	}
	
	private void cleanUpProcessedEvents() {
		List<Entry<String, UpdateFileReplicas>> toRemove = new LinkedList<Entry<String, UpdateFileReplicas>>();
		for(Entry<String, UpdateFileReplicas> entry : scheduledUpdateReplicasEvents.entrySet()) {
			if(entry.getValue().wasProcessed()) {
				toRemove.add(entry);
			}
		}
		scheduledUpdateReplicasEvents.entrySet().removeAll(toRemove);
	}

	public void updateReplicas(ReplicatedFile file) {
		replicator.updateReplicas(file);
	}
	
	public void delete(String filePath) {
		ReplicatedFile file = files.remove(filePath);
		if(file != null) {
			file.delete();
		}
	}

	public ReplicatedFile createOrOpen(FileSystemClient client, String path, long size) {
		ReplicatedFile theFile = files.get(path);
		
		if(theFile == null) {
			theFile = createFile(client, path, size);
		}
		
		return theFile;
	}

	private ReplicatedFile createFile(FileSystemClient client, String fullpath, long size) {
		ReplicatedFile newFile = dataPlacement.createFile(client, fullpath, replicationLevel, size);
		
		files.put(fullpath, newFile);
		
		return newFile;
	}
	
	public DataServer getDataServer(String host) {
		return dataServerByHost.get(host);
	}

}
