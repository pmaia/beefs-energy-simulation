package simulation.beefs.model;

import java.util.HashMap;
import java.util.Iterator;
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
	
	private static DataServer ds1, ds2, ds3, ds4, ds5, ds6, ds7, ds8, ds9, ds10, ds11;
	
	private static long minFreeSpace = Long.MAX_VALUE;

	public MetadataServer(Set<DataServer> dataServers, DataPlacement dataPlacementStrategy, 
			Replicator replicator, int replicationLevel, Time timeToCoherence) {
		
		for(DataServer dataServer : dataServers) {
			dataServerByHost.put(dataServer.getHost().getName(), dataServer);
		}
		
		this.dataPlacement = dataPlacementStrategy;
		this.replicator = replicator;
		this.replicationLevel = replicationLevel;
		this.timeToCoherence = timeToCoherence;
		
		Iterator<DataServer> dsIterator = dataServers.iterator();
		
		ds1 = dsIterator.next();
		ds2 = dsIterator.next();
		ds3 = dsIterator.next();
		ds4 = dsIterator.next();
		ds5 = dsIterator.next();
		ds6 = dsIterator.next();
		ds7 = dsIterator.next();
		ds8 = dsIterator.next();
		ds9 = dsIterator.next();
		ds10 = dsIterator.next();
		ds11 = dsIterator.next();
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

	public ReplicatedFile createOrOpen(FileSystemClient client, String path) {
		ReplicatedFile theFile = files.get(path);
		
		if(theFile == null) {
			theFile = createFile(client, path);
		}
		
		return theFile;
	}

	private ReplicatedFile createFile(FileSystemClient client, String fullpath) {
		ReplicatedFile newFile = dataPlacement.createFile(client, fullpath, replicationLevel);
		
		files.put(fullpath, newFile);
		
		return newFile;
	}
	
	public DataServer getDataServer(String host) {
		return dataServerByHost.get(host);
	}
	
	public static void notifyDiskUseChange() {
		long currentFreeSpace = ds1.freeSpace() + ds2.freeSpace() +ds3.freeSpace() +ds4.freeSpace() +ds5.freeSpace() +ds6.freeSpace() +ds7.freeSpace() +
				ds8.freeSpace() +ds9.freeSpace() +ds10.freeSpace() +ds11.freeSpace();
		
		minFreeSpace = Math.min(minFreeSpace, currentFreeSpace);
	}
	
	public static long minFreeSpace() {
		return minFreeSpace;
	}

}
