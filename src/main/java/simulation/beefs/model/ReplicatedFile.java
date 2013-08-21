package simulation.beefs.model;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;

public class ReplicatedFile {
	
	private final String fullpath;
	private final DataServer primary;
	private final int expectedReplicationLevel;
	
	private long size = 0;
	private long bytesWritten = 0;
	private boolean replicasAreConsistent = true;
	private Set<FileReplica> replicas;
	
	public ReplicatedFile(String fullpath, DataServer primary, int expectedReplicationLevel, Set<FileReplica> replicas) {
		this.fullpath = fullpath;
		this.primary = primary;
		this.replicas = replicas;
		this.expectedReplicationLevel = expectedReplicationLevel;
	}
	
	public void delete() {
		primary.cleanSpace(bytesWritten);
		for(FileReplica replica : replicas) {
			replica.delete();
		}
	}
	
	public void updateReplicas(Set<FileReplica> replicas) {
		this.replicas = replicas;
		replicasAreConsistent = true;
	}
	
	public void write(long bytes, long currentFileSize) {
		if(primary.freeSpace() < bytes) {
			String msg = String.format("disk full: write failed in %s (%d %d) - %s", primary.getHost().getName(), bytes, primary.freeSpace(), EventScheduler.now());
			System.out.println(msg);
			return;
		}
		primary.useDisk(bytes); //i am considering that every write is a append. this is the worst case scenario.
		
		size = currentFileSize + bytes;
		bytesWritten += bytes;
		replicasAreConsistent = false;
	}

	public DataServer primary() {
		return primary;
	}
	
	public Set<FileReplica> replicas() {
		return new HashSet<FileReplica>(replicas);
	}
	
	public String fullPath() {
		return fullpath;
	}
	
	public long bytesWritten() {
		return bytesWritten;
	}
	
	public long size() {
		return size;
	}
	
	public boolean areReplicasConsistent() {
		return replicasAreConsistent;
	}
	
	public int actualReplicationLevel() {
		return replicas.size();
	}

	public int expectedReplicationLevel() {
		return expectedReplicationLevel;
	}

}
